package com.fsrspring.vocab.service;

import com.fsrspring.vocab.model.TrustedFlashcard;
import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.repository.TrustedFlashcardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class FlashcardService {

    private static final Map<String, String> LEGACY_TRANSLATION_FIXES = Map.of(
            "kien cuong", "kiên cường",
            "dien dat ro rang", "diễn đạt rõ ràng",
            "su thau hieu", "sự thấu hiểu"
    );

    private final TrustedFlashcardRepository flashcardRepository;
    private final WordService wordService;

    public List<TrustedFlashcard> list(String search) {
        normalizeLegacyTranslations();
        if (search != null && !search.isBlank()) {
            return flashcardRepository.searchByKeyword(search);
        }
        return flashcardRepository.findTop100ByOrderByImportedAtDesc();
    }

    public List<TrustedFlashcard> importTrustedSet(String sourceName, String topic) {
        List<TrustedFlashcard> seeded = new ArrayList<>();

        List<Map<String, String>> defaults = List.of(
            Map.of("word", "resilient", "translation", "kiên cường", "example", "She is resilient in difficult times."),
            Map.of("word", "articulate", "translation", "diễn đạt rõ ràng", "example", "He can articulate complex ideas clearly."),
            Map.of("word", "insight", "translation", "sự thấu hiểu", "example", "The report provides useful insight.")
        );

        for (Map<String, String> item : defaults) {
            TrustedFlashcard flashcard = TrustedFlashcard.builder()
                    .word(item.get("word"))
                    .translation(item.get("translation"))
                    .example(item.get("example"))
                    .topic(topic)
                    .level("B1")
                    .sourceName(sourceName)
                    .sourceUrl("https://" + sourceName.toLowerCase().replace(" ", "") + ".com")
                    .build();
            seeded.add(flashcardRepository.save(flashcard));
        }

        return seeded;
    }

    public Word saveToVocabulary(Long flashcardId, Word.DifficultyLevel difficulty, String category) {
        TrustedFlashcard flashcard = flashcardRepository.findById(flashcardId)
                .orElseThrow(() -> new IllegalArgumentException("Flashcard not found: " + flashcardId));

        Word word = Word.builder()
                .word(flashcard.getWord())
                .translation(flashcard.getTranslation())
                .example(flashcard.getExample())
                .difficulty(difficulty)
                .category(category)
                .build();

        return wordService.createWord(word);
    }

    private void normalizeLegacyTranslations() {
        List<TrustedFlashcard> candidates = flashcardRepository.findTop100ByOrderByImportedAtDesc();
        for (TrustedFlashcard flashcard : candidates) {
            String raw = flashcard.getTranslation();
            if (raw == null || raw.isBlank()) {
                continue;
            }

            String normalized = LEGACY_TRANSLATION_FIXES.get(raw.trim().toLowerCase(Locale.ROOT));
            if (normalized != null && !normalized.equals(raw)) {
                flashcard.setTranslation(normalized);
                flashcardRepository.save(flashcard);
            }
        }
    }
}
