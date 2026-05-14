package com.fsrspring.vocab.controller;

import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.service.WordService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/words")
@RequiredArgsConstructor
public class WordController {

    private final WordService wordService;

    @GetMapping
    public ResponseEntity<?> getAllWords(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Word.DifficultyLevel difficulty,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long topicId,
            @RequestParam(required = false) com.fsrspring.vocab.model.CefrLevel cefrLevel,
            @RequestParam(required = false) String partOfSpeech,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer size) {
        if (page != null || offset != null || size != null) {
            int responseSize = size == null ? 20 : size;
            int responseOffset = offset == null ? (page == null ? 0 : page * responseSize) : offset;
            Page<Word> wordPage = wordService.getWordsPage(
                    category,
                    difficulty,
                    search,
                    topicId,
                    cefrLevel,
                    partOfSpeech,
                    responseOffset,
                    responseSize);
            return ResponseEntity.ok(new WordPageResponse(
                    wordPage.getContent(),
                    wordPage.getNumber(),
                    wordPage.getSize(),
                    wordPage.getTotalElements(),
                    wordPage.getTotalPages(),
                    wordPage.isLast()));
        }

        List<Word> words;
        if (search != null && !search.isBlank()) {
            words = wordService.searchWords(search);
        } else if (topicId != null && cefrLevel != null) {
            words = wordService.getWordsByTopicAndCefr(topicId, cefrLevel);
        } else if (topicId != null) {
            words = wordService.getWordsByTopic(topicId);
        } else if (cefrLevel != null) {
            words = wordService.getWordsByCefrLevel(cefrLevel);
        } else if (partOfSpeech != null) {
            words = wordService.getWordsByPartOfSpeech(partOfSpeech);
        } else if (category != null && difficulty != null) {
            words = wordService.getWordsByCategoryAndDifficulty(category, difficulty);
        } else if (category != null) {
            words = wordService.getWordsByCategory(category);
        } else if (difficulty != null) {
            words = wordService.getWordsByDifficulty(difficulty);
        } else {
            words = wordService.getAllWords();
        }
        return ResponseEntity.ok(words);
    }

    public record WordPageResponse(
            List<Word> content,
            int number,
            int size,
            long totalElements,
            int totalPages,
            boolean last) {
    }

    @GetMapping("/{id}")
    public ResponseEntity<Word> getWordById(@PathVariable Long id) {
        return ResponseEntity.ok(wordService.getWordById(id));
    }

    @GetMapping("/random")
    public ResponseEntity<List<Word>> getRandomWords(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(wordService.getRandomWords(limit));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(wordService.getAllCategories());
    }

    @GetMapping("/parts-of-speech")
    public ResponseEntity<List<String>> getPartsOfSpeech() {
        return ResponseEntity.ok(wordService.getAllPartsOfSpeech());
    }

    @PostMapping
    public ResponseEntity<Word> createWord(@Valid @RequestBody Word word) {
        return ResponseEntity.status(HttpStatus.CREATED).body(wordService.createWord(word));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Word> updateWord(@PathVariable Long id, @Valid @RequestBody Word word) {
        return ResponseEntity.ok(wordService.updateWord(id, word));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWord(@PathVariable Long id) {
        wordService.deleteWord(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getWordCount() {
        return ResponseEntity.ok(Map.of("count", wordService.countWords()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
    }
}
