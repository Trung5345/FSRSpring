package com.fsrspring.vocab.service;

import com.fsrspring.vocab.dto.DictionaryApiResponse;
import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final DictionaryApiService dictionaryApiService;
    private final DatamuseApiService datamuseApiService;

    /**
     * Enrich a single word by ID — fetches from Free Dictionary API and Datamuse,
     * then patches the entity fields that are blank.
     */
    @Transactional
    public Word enrichWord(Long wordId) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new IllegalArgumentException("Word not found: " + wordId));

        Optional<DictionaryApiResponse> apiResponse = dictionaryApiService.lookup(word.getWord());

        apiResponse.ifPresent(res -> {
            if (word.getPronunciation() == null || word.getPronunciation().isBlank()) {
                word.setPronunciation(res.getBestPhonetic());
            }
            if (word.getAudioUrl() == null || word.getAudioUrl().isBlank()) {
                word.setAudioUrl(res.getBestAudioUrl());
            }
            if (word.getPartOfSpeech() == null || word.getPartOfSpeech().isBlank()) {
                word.setPartOfSpeech(res.getPrimaryPartOfSpeech());
            }
            if (word.getExample() == null || word.getExample().isBlank()) {
                word.setExample(res.getFirstExample());
            }
            if (word.getSynonyms() == null || word.getSynonyms().isBlank()) {
                word.setSynonyms(res.getAllSynonyms());
            }
            if (word.getAntonyms() == null || word.getAntonyms().isBlank()) {
                word.setAntonyms(res.getAllAntonyms());
            }
        });

        // Supplement synonyms/antonyms from Datamuse if still empty
        if (word.getSynonyms() == null || word.getSynonyms().isBlank()) {
            List<String> syns = datamuseApiService.getSynonyms(word.getWord());
            if (!syns.isEmpty()) {
                word.setSynonyms(String.join(", ", syns));
            }
        }
        if (word.getAntonyms() == null || word.getAntonyms().isBlank()) {
            List<String> ants = datamuseApiService.getAntonyms(word.getWord());
            if (!ants.isEmpty()) {
                word.setAntonyms(String.join(", ", ants));
            }
        }

        return wordRepository.save(word);
    }

    /**
     * Enrich all words that are missing enrichment data (audio, pronunciation, etc.)
     */
    @Transactional
    public int enrichAllMissing() {
        List<Word> words = wordRepository.findAll();
        int count = 0;
        for (Word word : words) {
            boolean missing = word.getAudioUrl() == null || word.getPronunciation() == null
                    || word.getSynonyms() == null || word.getPartOfSpeech() == null;
            if (missing) {
                try {
                    enrichWord(word.getId());
                    count++;
                    Thread.sleep(200); // be polite to the API
                } catch (Exception e) {
                    log.warn("Failed to enrich word '{}': {}", word.getWord(), e.getMessage());
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
}
