package com.fsrspring.vocab.service;

import com.fsrspring.vocab.dto.DatamuseWordDto;
import com.fsrspring.vocab.dto.DictionaryApiResponse;
import com.fsrspring.vocab.model.TrustedFlashcard;
import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.repository.TrustedFlashcardRepository;
import com.fsrspring.vocab.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class FlashcardService {

    private static final int IMPORT_BATCH_SIZE = 6;
    private static final int CANDIDATE_POOL_SIZE = 50;

    private static final Map<String, String> LEGACY_TRANSLATION_FIXES = Map.of(
            "kien cuong", "kiên cường",
            "dien dat ro rang", "diễn đạt rõ ràng",
            "su thau hieu", "sự thấu hiểu"
    );

    private final TrustedFlashcardRepository flashcardRepository;
    private final WordService wordService;
    private final WordRepository wordRepository;
    private final DatamuseApiService datamuseApiService;
    private final DictionaryApiService dictionaryApiService;
    private final LibreTranslateClient libreTranslateClient;
    private final ProgressService progressService;

    public List<TrustedFlashcard> list(String search) {
        normalizeLegacyTranslations();
        List<TrustedFlashcard> cards;
        if (search != null && !search.isBlank()) {
            cards = flashcardRepository.searchByKeyword(search);
        } else {
            cards = flashcardRepository.findTop100ByOrderByImportedAtDesc();
        }
        return uniqueByWord(cards);
    }

    public List<TrustedFlashcard> importTrustedSet(String sourceName, String topic) {
        String safeSource = clean(sourceName, "External Vocabulary APIs");
        String safeTopic = normalizeTopic(topic);
        List<TrustedFlashcard> imported = new ArrayList<>();

        for (String candidate : candidateWords(safeTopic)) {
            if (imported.size() >= IMPORT_BATCH_SIZE) {
                break;
            }
            if (flashcardRepository.existsByWordIgnoreCase(candidate) || wordRepository.existsByWordIgnoreCase(candidate)) {
                continue;
            }

            buildTrustedFlashcard(candidate, safeSource, safeTopic)
                    .map(flashcardRepository::save)
                    .ifPresent(imported::add);
        }

        if (imported.isEmpty()) {
            throw new IllegalStateException("No new flashcards were returned by external providers for topic: " + safeTopic);
        }

        return uniqueByWord(imported);
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

        Word saved = wordService.getOrCreateWord(word);
        progressService.getOrCreateProgress(saved);
        return saved;
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

    private List<String> candidateWords(String topic) {
        Map<String, String> unique = new LinkedHashMap<>();
        for (DatamuseWordDto dto : datamuseApiService.getWordsByTopic(topic, CANDIDATE_POOL_SIZE)) {
            addCandidate(unique, dto.getWord());
        }
        for (DatamuseWordDto dto : datamuseApiService.getWordsMeaningLike(topic, CANDIDATE_POOL_SIZE)) {
            addCandidate(unique, dto.getWord());
        }
        return new ArrayList<>(unique.values());
    }

    private void addCandidate(Map<String, String> unique, String rawWord) {
        String word = normalizeWord(rawWord);
        if (word != null) {
            unique.putIfAbsent(word.toLowerCase(Locale.ROOT), word);
        }
    }

    private Optional<TrustedFlashcard> buildTrustedFlashcard(String word, String sourceName, String topic) {
        Optional<DictionaryApiResponse> dictionary = dictionaryApiService.lookup(word);
        String definition = dictionary.map(DictionaryApiResponse::getFirstDefinition).orElse(null);
        String example = dictionary.map(DictionaryApiResponse::getFirstExample).orElse(null);
        String translation = translate(word).or(() -> translate(definition)).orElse(definition);

        if (blank(translation) && blank(definition)) {
            return Optional.empty();
        }

        String sourceUrl = dictionary
                .map(DictionaryApiResponse::getSourceUrls)
                .filter(urls -> urls != null && !urls.isEmpty())
                .map(urls -> urls.get(0))
                .orElse("https://www.datamuse.com/api/");

        return Optional.of(TrustedFlashcard.builder()
                .word(word)
                .translation(limit(firstNonBlank(translation, definition), 500))
                .example(limit(example, 1000))
                .topic(topic)
                .level("B1")
                .sourceName(sourceName)
                .sourceUrl(sourceUrl)
                .build());
    }

    private Optional<String> translate(String value) {
        if (blank(value)) {
            return Optional.empty();
        }
        return libreTranslateClient.translateEnToVi(value)
                .filter(translated -> !blank(translated) && !translated.equalsIgnoreCase(value));
    }

    private String normalizeTopic(String value) {
        String cleaned = clean(value, "vocabulary");
        return "general".equalsIgnoreCase(cleaned) ? "vocabulary" : cleaned;
    }

    private String normalizeWord(String value) {
        if (blank(value)) {
            return null;
        }
        String cleaned = value.trim().toLowerCase(Locale.ROOT);
        if (cleaned.contains(" ") || !cleaned.matches("[a-z][a-z'-]{1,79}")) {
            return null;
        }
        return cleaned;
    }

    private String clean(String value, String fallback) {
        return blank(value) ? fallback : value.trim();
    }

    private String firstNonBlank(String first, String second) {
        return !blank(first) ? first : second;
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

    private List<TrustedFlashcard> uniqueByWord(List<TrustedFlashcard> cards) {
        Map<String, TrustedFlashcard> unique = new LinkedHashMap<>();
        for (TrustedFlashcard card : cards) {
            String key = card.getWord() == null ? "" : card.getWord().trim().toLowerCase(Locale.ROOT);
            if (!key.isBlank()) {
                unique.putIfAbsent(key, card);
            }
        }
        return new ArrayList<>(unique.values());
    }
}
