package com.fsrspring.vocab.service;

import com.fsrspring.vocab.dto.ImportCommitRequest;
import com.fsrspring.vocab.dto.ImportCommitResponse;
import com.fsrspring.vocab.model.ImportJob;
import com.fsrspring.vocab.model.ImportJobRow;
import com.fsrspring.vocab.model.Topic;
import com.fsrspring.vocab.model.VocabularySet;
import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.repository.ImportJobRepository;
import com.fsrspring.vocab.repository.TopicRepository;
import com.fsrspring.vocab.repository.VocabularySetRepository;
import com.fsrspring.vocab.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ImportService {

    private final ImportJobRepository importJobRepository;
    private final WordRepository wordRepository;
    private final TopicRepository topicRepository;
    private final VocabularySetRepository vocabularySetRepository;
    private final WordEnrichmentService wordEnrichmentService;

    @Transactional
    public ImportCommitResponse commitWords(ImportCommitRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Import request is required");
        }
        List<ImportCommitRequest.Row> requestRows =
                request.getRows() != null ? request.getRows() : List.of();
        VocabularySet targetSet = resolveTargetSet(request);

        ImportJob job = ImportJob.builder()
                .sourceType(blankToDefault(request.getSourceType(), "UNKNOWN"))
                .fileName(clean(request.getFileName()))
                .targetSetId(targetSet != null ? targetSet.getId() : null)
                .targetSetName(targetSet != null ? targetSet.getName() : null)
                .totalRows(requestRows.size())
                .build();

        List<ImportCommitResponse.RowResult> responseRows = new ArrayList<>();
        boolean autoEnrich = autoEnrich(request.getOptions());
        int created = 0;
        int skipped = 0;
        int failed = 0;

        for (int i = 0; i < requestRows.size(); i++) {
            ImportCommitRequest.Row row = requestRows.get(i);
            RowOutcome outcome = importRow(row, i + 1, targetSet, autoEnrich);
            job.addRow(outcome.jobRow());
            responseRows.add(outcome.responseRow());

            switch (outcome.jobRow().getStatus()) {
                case CREATED -> created++;
                case SKIPPED -> skipped++;
                case FAILED -> failed++;
            }
        }

        job.setCreatedCount(created);
        job.setSkippedCount(skipped);
        job.setFailedCount(failed);
        job.setStatus(failed > 0 ? ImportJob.Status.COMPLETED_WITH_ERRORS : ImportJob.Status.COMPLETED);

        ImportJob saved = importJobRepository.save(job);
        if (targetSet != null) {
            vocabularySetRepository.save(targetSet);
        }

        return ImportCommitResponse.builder()
                .importJobId(saved.getId())
                .created(created)
                .skipped(skipped)
                .failed(failed)
                .rows(responseRows)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ImportJob> listJobs() {
        return importJobRepository.findTop50ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public ImportJob getJob(Long id) {
        return importJobRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Import job not found: " + id));
    }

    private RowOutcome importRow(ImportCommitRequest.Row row, int rowNumber, VocabularySet targetSet, boolean autoEnrich) {
        if (row == null) {
            ImportJobRow.ImportJobRowBuilder jobRow = ImportJobRow.builder()
                    .rowNumber(rowNumber)
                    .wordText("");
            return outcome(jobRow, null, "", ImportJobRow.Status.FAILED, null, "Row is required");
        }
        String clientRowId = clean(row.getClientRowId());
        String wordText = normalizeWord(row.getWord());
        String translation = clean(row.getTranslation());

        ImportJobRow.ImportJobRowBuilder jobRow = ImportJobRow.builder()
                .rowNumber(rowNumber)
                .clientRowId(clientRowId)
                .wordText(wordText != null ? wordText : "")
                .translation(translation);

        if (wordText == null || wordText.isBlank()) {
            return outcome(jobRow, clientRowId, "", ImportJobRow.Status.FAILED, null, "Word is required");
        }
        if (wordText.length() > 200) {
            return outcome(jobRow, clientRowId, wordText, ImportJobRow.Status.FAILED, null, "Word exceeds 200 characters");
        }
        if ((translation == null || translation.isBlank()) && !autoEnrich) {
            return outcome(jobRow, clientRowId, wordText, ImportJobRow.Status.FAILED, null, "Translation is required");
        }
        if (translation != null && translation.length() > 500) {
            return outcome(jobRow, clientRowId, wordText, ImportJobRow.Status.FAILED, null, "Translation exceeds 500 characters");
        }
        if (wordRepository.existsByWordIgnoreCase(wordText)) {
            return outcome(jobRow, clientRowId, wordText, ImportJobRow.Status.SKIPPED, null, "Duplicate word skipped");
        }

        Topic topic = row.getTopicId() != null
                ? topicRepository.findById(row.getTopicId()).orElse(null)
                : null;

        Word word = Word.builder()
                .word(wordText)
                .translation(translation)
                .example(limit(clean(row.getExample()), 1000))
                .pronunciation(limit(clean(row.getPronunciation()), 200))
                .category(limit(clean(row.getCategory()), 100))
                .difficulty(row.getDifficulty() != null ? row.getDifficulty() : Word.DifficultyLevel.BEGINNER)
                .topic(topic)
                .cefrLevel(row.getCefrLevel())
                .partOfSpeech(limit(clean(row.getPartOfSpeech()), 50))
                .audioUrl(limit(clean(row.getAudioUrl()), 500))
                .build();

        Word saved = wordRepository.save(word);
        String enrichmentStatus = null;
        String message = "Created";
        if (autoEnrich) {
            wordEnrichmentService.enqueueWord(saved.getId());
            enrichmentStatus = Word.EnrichmentStatus.PENDING.name();
            message = "Created; enrichment queued";
        }
        if (targetSet != null) {
            ensureSetWords(targetSet);
            targetSet.getWords().add(saved);
        }

        return outcome(jobRow, clientRowId, wordText, ImportJobRow.Status.CREATED, saved.getId(), enrichmentStatus, message);
    }

    private VocabularySet resolveTargetSet(ImportCommitRequest request) {
        ImportCommitRequest.TargetSet targetSetRequest = request.getTargetSet();
        if (targetSetRequest == null && clean(request.getTargetSetName()) != null) {
            targetSetRequest = new ImportCommitRequest.TargetSet();
            targetSetRequest.setName(request.getTargetSetName());
        }
        if (targetSetRequest == null) {
            return null;
        }
        if (targetSetRequest.getId() != null) {
            Long targetSetId = targetSetRequest.getId();
            VocabularySet set = vocabularySetRepository.findById(targetSetId)
                    .orElseThrow(() -> new IllegalArgumentException("Target set not found: " + targetSetId));
            ensureSetWords(set);
            return set;
        }
        String name = clean(targetSetRequest.getName());
        if (name == null || name.isBlank()) {
            return null;
        }
        Topic topic = targetSetRequest.getTopicId() != null
                ? topicRepository.findById(targetSetRequest.getTopicId()).orElse(null)
                : null;
        VocabularySet set = VocabularySet.builder()
                .name(limit(name, 200))
                .description(limit(clean(targetSetRequest.getDescription()), 1000))
                .topic(topic)
                .cefrLevel(targetSetRequest.getCefrLevel())
                .build();
        ensureSetWords(set);
        return vocabularySetRepository.save(set);
    }

    private RowOutcome outcome(ImportJobRow.ImportJobRowBuilder jobRowBuilder,
                               String clientRowId,
                               String word,
                               ImportJobRow.Status status,
                               Long wordId,
                               String message) {
        return outcome(jobRowBuilder, clientRowId, word, status, wordId, null, message);
    }

    private RowOutcome outcome(ImportJobRow.ImportJobRowBuilder jobRowBuilder,
                               String clientRowId,
                               String word,
                               ImportJobRow.Status status,
                               Long wordId,
                               String enrichmentStatus,
                               String message) {
        ImportJobRow jobRow = jobRowBuilder
                .status(status)
                .wordId(wordId)
                .message(message)
                .build();
        ImportCommitResponse.RowResult responseRow = ImportCommitResponse.RowResult.builder()
                .clientRowId(clientRowId)
                .word(word)
                .status(status)
                .wordId(wordId)
                .enrichmentStatus(enrichmentStatus)
                .message(message)
                .build();
        return new RowOutcome(jobRow, responseRow);
    }

    private boolean autoEnrich(java.util.Map<String, Object> options) {
        if (options == null || !options.containsKey("autoEnrich")) {
            return true;
        }
        Object value = options.get("autoEnrich");
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private void ensureSetWords(VocabularySet set) {
        if (set.getWords() == null) {
            set.setWords(new HashSet<>());
        }
    }

    private String normalizeWord(String value) {
        return clean(value);
    }

    private String blankToDefault(String value, String defaultValue) {
        String cleaned = clean(value);
        return cleaned == null || cleaned.isBlank() ? defaultValue : limit(cleaned, 50);
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record RowOutcome(ImportJobRow jobRow, ImportCommitResponse.RowResult responseRow) {
    }
}
