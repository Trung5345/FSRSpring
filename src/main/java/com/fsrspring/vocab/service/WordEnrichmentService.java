package com.fsrspring.vocab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fsrspring.vocab.dto.DictionaryApiResponse;
import com.fsrspring.vocab.dto.ProviderWordData;
import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.model.WordEnrichmentJob;
import com.fsrspring.vocab.config.EnrichmentProperties;
import com.fsrspring.vocab.repository.WordEnrichmentJobRepository;
import com.fsrspring.vocab.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Enriches Word entities with data from external dictionary APIs.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WordEnrichmentService {

    private final WordRepository wordRepository;
    private final WordEnrichmentJobRepository enrichmentJobRepository;
    private final EnrichmentProperties enrichmentProperties;
    private final ObjectMapper objectMapper;
    private final LibreTranslateClient libreTranslateClient;
    private final WiktApiClient wiktApiClient;
    private final DictionaryApiService dictionaryApiService;
    private final DatamuseApiService datamuseApiService;
    private final WordImageService wordImageService;

    @Autowired @Lazy
    private WordEnrichmentService self;

    /**
     * Compatibility method for older callers. It creates or reuses an enrichment
     * job, processes it immediately, and returns the updated word.
     */
    @Transactional
    public Word enrichWord(Long wordId) {
        WordEnrichmentJob job = enqueueWord(wordId);
        self.processJobNow(job.getId());
        return wordRepository.findById(wordId)
                .orElseThrow(() -> new IllegalArgumentException("Word not found: " + wordId));
    }

    @Transactional
    public WordEnrichmentJob enqueueWord(Long wordId) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new IllegalArgumentException("Word not found: " + wordId));

        if (!enrichmentProperties.getEnrichment().isEnabled()) {
            word.setEnrichmentStatus(Word.EnrichmentStatus.NOT_REQUESTED);
            wordRepository.save(word);
            return enrichmentJobRepository.findTopByWordIdOrderByCreatedAtDesc(wordId)
                    .orElseGet(() -> WordEnrichmentJob.builder()
                            .word(word)
                            .status(WordEnrichmentJob.Status.COMPLETED)
                            .completedAt(LocalDateTime.now())
                            .build());
        }

        List<WordEnrichmentJob.Status> active = List.of(WordEnrichmentJob.Status.PENDING, WordEnrichmentJob.Status.RUNNING);
        if (enrichmentJobRepository.existsByWordIdAndStatusIn(wordId, active)) {
            return enrichmentJobRepository.findTopByWordIdOrderByCreatedAtDesc(wordId)
                    .orElseThrow(() -> new IllegalStateException("Active enrichment job not found"));
        }

        word.setEnrichmentStatus(Word.EnrichmentStatus.PENDING);
        wordRepository.save(word);

        return enrichmentJobRepository.save(WordEnrichmentJob.builder()
                .word(word)
                .status(WordEnrichmentJob.Status.PENDING)
                .nextRunAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public WordEnrichmentJob retryWord(Long wordId) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new IllegalArgumentException("Word not found: " + wordId));
        word.setEnrichmentStatus(Word.EnrichmentStatus.PENDING);
        wordRepository.save(word);
        return enrichmentJobRepository.save(WordEnrichmentJob.builder()
                .word(word)
                .status(WordEnrichmentJob.Status.PENDING)
                .nextRunAt(LocalDateTime.now())
                .build());
    }

    @Transactional(readOnly = true)
    public Optional<WordEnrichmentJob> latestJob(Long wordId) {
        return enrichmentJobRepository.findTopByWordIdOrderByCreatedAtDesc(wordId);
    }

    @Scheduled(fixedDelayString = "${app.enrichment.poll-delay-ms:10000}")
    public void processDueJobs() {
        if (!enrichmentProperties.getEnrichment().isEnabled()) {
            return;
        }
        List<Long> dueJobIds = enrichmentJobRepository
                .findTop10ByStatusAndNextRunAtLessThanEqualOrderByCreatedAtAsc(
                        WordEnrichmentJob.Status.PENDING,
                        LocalDateTime.now()
                )
                .stream().map(WordEnrichmentJob::getId).toList();
        for (Long jobId : dueJobIds) {
            try {
                self.processJobNow(jobId);
            } catch (Exception e) {
                log.warn("Enrichment job {} failed: {}", jobId, e.getMessage());
            }
        }
    }

    @Transactional
    public void processJobNow(Long jobId) {
        WordEnrichmentJob job = enrichmentJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Enrichment job not found: " + jobId));
        processLoadedJob(job);
    }

    private void processLoadedJob(WordEnrichmentJob job) {
        Word word = job.getWord();
        LocalDateTime now = LocalDateTime.now();
        job.setStatus(WordEnrichmentJob.Status.RUNNING);
        job.setAttempts(job.getAttempts() + 1);
        job.setStartedAt(now);
        job.setUpdatedAt(now);
        job.setLastError(null);
        word.setEnrichmentStatus(Word.EnrichmentStatus.RUNNING);
        enrichmentJobRepository.save(job);
        wordRepository.save(word);

        EnrichmentMerge merge = new EnrichmentMerge(word);
        List<String> warnings = new ArrayList<>();
        ObjectNode raw = objectMapper.createObjectNode();

        try {
            if (blank(word.getTranslation())) {
                libreTranslateClient.translateEnToVi(word.getWord()).ifPresent(value -> merge.setTranslation(limit(value, 500)));
            }

            ProviderWordData wiktData = wiktApiClient.lookupEnglish(word.getWord());
            if (wiktData.getRawJson() != null) {
                raw.set("wiktapi", wiktData.getRawJson());
            }
            merge.applyProviderData(wiktData);

            applyDictionaryFallback(word, merge, raw);
            applyDatamuseFallback(word, merge);
            applyImage(word, merge, warnings, raw);

            WordEnrichmentJob.Status finalStatus = finalStatus(merge.changed(), warnings);
            job.setStatus(finalStatus);
            job.setCompletedAt(LocalDateTime.now());
            job.setUpdatedAt(LocalDateTime.now());
            job.setLastError(warnings.isEmpty() ? null : limit(String.join("; ", warnings), 1000));
            word.setEnrichmentStatus(toWordStatus(finalStatus));
            word.setEnrichedAt(LocalDateTime.now());
            if (raw.size() > 0) {
                word.setEnrichmentJson(raw.toString());
            }
            wordRepository.save(word);
            enrichmentJobRepository.save(job);
        } catch (Exception e) {
            job.setStatus(merge.changed() ? WordEnrichmentJob.Status.PARTIAL : WordEnrichmentJob.Status.FAILED);
            job.setCompletedAt(LocalDateTime.now());
            job.setUpdatedAt(LocalDateTime.now());
            job.setLastError(limit(e.getMessage(), 1000));
            word.setEnrichmentStatus(toWordStatus(job.getStatus()));
            word.setEnrichedAt(LocalDateTime.now());
            wordRepository.save(word);
            enrichmentJobRepository.save(job);
            log.warn("Failed to enrich word '{}': {}", word.getWord(), e.getMessage());
        }
    }

    /**
     * Enqueue all words that are missing enrichment data (audio, pronunciation, etc.)
     */
    @Transactional
    public int enrichAllMissing() {
        List<Word> words = wordRepository.findAll();
        int count = 0;
        for (Word word : words) {
            boolean missing = blank(word.getTranslation()) || blank(word.getAudioUrl()) || blank(word.getPronunciation())
                    || blank(word.getSynonyms()) || blank(word.getPartOfSpeech()) || blank(word.getImageUrl());
            if (missing) {
                try {
                    enqueueWord(word.getId());
                    count++;
                } catch (Exception e) {
                    log.warn("Failed to enqueue enrichment for word '{}': {}", word.getWord(), e.getMessage());
                }
            }
        }
        return count;
    }

    /**
     * Preview lookup without saving — for the "lookup" endpoint that shows
     * dictionary info without modifying the DB.
     */
    public Optional<DictionaryApiResponse> previewLookup(String wordText) {
        return dictionaryApiService.lookup(wordText);
    }

    private void applyDictionaryFallback(Word word, EnrichmentMerge merge, ObjectNode raw) {
        boolean needsDictionary = blank(word.getPronunciation()) || blank(word.getAudioUrl()) || blank(word.getPartOfSpeech())
                || blank(word.getExample()) || blank(word.getSynonyms()) || blank(word.getAntonyms());
        if (!needsDictionary) {
            return;
        }
        dictionaryApiService.lookup(word.getWord()).ifPresent(res -> {
            raw.putPOJO("freeDictionary", res);
            merge.setPronunciation(limit(res.getBestPhonetic(), 200));
            merge.setAudioUrl(limit(normalizeAudio(res.getBestAudioUrl()), 500));
            merge.setPartOfSpeech(limit(res.getPrimaryPartOfSpeech(), 50));
            merge.setExample(limit(res.getFirstExample(), 1000));
            merge.setSynonyms(limit(res.getAllSynonyms(), 1000));
            merge.setAntonyms(limit(res.getAllAntonyms(), 1000));
        });
    }

    private void applyDatamuseFallback(Word word, EnrichmentMerge merge) {
        if (blank(word.getSynonyms())) {
            List<String> syns = datamuseApiService.getSynonyms(word.getWord());
            if (!syns.isEmpty()) {
                merge.setSynonyms(limit(String.join(", ", syns), 1000));
            }
        }
        if (blank(word.getAntonyms())) {
            List<String> ants = datamuseApiService.getAntonyms(word.getWord());
            if (!ants.isEmpty()) {
                merge.setAntonyms(limit(String.join(", ", ants), 1000));
            }
        }
    }

    private void applyImage(Word word, EnrichmentMerge merge, List<String> warnings, ObjectNode raw) {
        if (!blank(word.getImageUrl())) {
            return;
        }
        try {
            wordImageService.findAndStoreImage(word.getWord()).ifPresent(image -> {
                merge.setImageUrl(limit(image.imageUrl(), 500));
                word.setImageMetadataJson(image.metadataJson());
                raw.put("imageMetadata", image.metadataJson());
            });
        } catch (Exception e) {
            warnings.add("image: " + e.getMessage());
        }
    }

    private WordEnrichmentJob.Status finalStatus(boolean changed, List<String> warnings) {
        if (!changed && !warnings.isEmpty()) {
            return WordEnrichmentJob.Status.FAILED;
        }
        if (!warnings.isEmpty()) {
            return WordEnrichmentJob.Status.PARTIAL;
        }
        return WordEnrichmentJob.Status.COMPLETED;
    }

    private Word.EnrichmentStatus toWordStatus(WordEnrichmentJob.Status status) {
        return switch (status) {
            case PENDING -> Word.EnrichmentStatus.PENDING;
            case RUNNING -> Word.EnrichmentStatus.RUNNING;
            case PARTIAL -> Word.EnrichmentStatus.PARTIAL;
            case COMPLETED -> Word.EnrichmentStatus.COMPLETED;
            case FAILED -> Word.EnrichmentStatus.FAILED;
        };
    }

    private String normalizeAudio(String url) {
        if (url == null) return null;
        return url.startsWith("//") ? "https:" + url : url;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private class EnrichmentMerge {
        private final Word word;
        private boolean changed;

        EnrichmentMerge(Word word) {
            this.word = word;
        }

        boolean changed() {
            return changed;
        }

        void applyProviderData(ProviderWordData data) {
            if (data == null) return;
            setTranslation(limit(data.getTranslation(), 500));
            setPronunciation(limit(data.getPronunciation(), 200));
            setAudioUrl(limit(normalizeAudio(data.getAudioUrl()), 500));
            setPartOfSpeech(limit(data.getPartOfSpeech(), 50));
            setExample(limit(firstNonBlank(data.getExample(), data.getDefinition()), 1000));
            setSynonyms(limit(data.getSynonyms(), 1000));
            setAntonyms(limit(data.getAntonyms(), 1000));
            setOrigin(limit(data.getOrigin(), 2000));
        }

        void setTranslation(String value) {
            if (blank(word.getTranslation()) && !blank(value)) {
                word.setTranslation(value);
                changed = true;
            }
        }

        void setPronunciation(String value) {
            if (blank(word.getPronunciation()) && !blank(value)) {
                word.setPronunciation(value);
                changed = true;
            }
        }

        void setAudioUrl(String value) {
            if (blank(word.getAudioUrl()) && !blank(value)) {
                word.setAudioUrl(value);
                changed = true;
            }
        }

        void setPartOfSpeech(String value) {
            if (blank(word.getPartOfSpeech()) && !blank(value)) {
                word.setPartOfSpeech(value);
                changed = true;
            }
        }

        void setExample(String value) {
            if (blank(word.getExample()) && !blank(value)) {
                word.setExample(value);
                changed = true;
            }
        }

        void setSynonyms(String value) {
            if (blank(word.getSynonyms()) && !blank(value)) {
                word.setSynonyms(value);
                changed = true;
            }
        }

        void setAntonyms(String value) {
            if (blank(word.getAntonyms()) && !blank(value)) {
                word.setAntonyms(value);
                changed = true;
            }
        }

        void setOrigin(String value) {
            if (blank(word.getOrigin()) && !blank(value)) {
                word.setOrigin(value);
                changed = true;
            }
        }

        void setImageUrl(String value) {
            if (blank(word.getImageUrl()) && !blank(value)) {
                word.setImageUrl(value);
                changed = true;
            }
        }

        private String firstNonBlank(String first, String second) {
            return !blank(first) ? first : second;
        }
    }
}
