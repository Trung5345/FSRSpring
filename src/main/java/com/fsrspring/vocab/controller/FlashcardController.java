package com.fsrspring.vocab.controller;

import com.fsrspring.vocab.model.TrustedFlashcard;
import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.service.FlashcardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flashcards")
@RequiredArgsConstructor
public class FlashcardController {

    private final FlashcardService flashcardService;

    @GetMapping
    public ResponseEntity<List<TrustedFlashcard>> list(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(flashcardService.list(search));
    }

    @PostMapping("/import")
    public ResponseEntity<List<TrustedFlashcard>> importTrusted(
            @RequestParam(defaultValue = "TrustedSource") String source,
            @RequestParam(defaultValue = "general") String topic) {
        return ResponseEntity.ok(flashcardService.importTrustedSet(source, topic));
    }

    @PostMapping("/{id}/save-to-vocab")
    public ResponseEntity<Word> saveToVocab(
            @PathVariable Long id,
            @RequestParam(defaultValue = "BEGINNER") Word.DifficultyLevel difficulty,
            @RequestParam(defaultValue = "Imported") String category) {
        return ResponseEntity.ok(flashcardService.saveToVocabulary(id, difficulty, category));
    }

    @GetMapping("/sources")
    public ResponseEntity<List<Map<String, String>>> sources() {
        return ResponseEntity.ok(List.of(
                Map.of("name", "BBC Learning English", "type", "trusted"),
                Map.of("name", "VOA Learning English", "type", "trusted"),
                Map.of("name", "Cambridge Dictionary Blog", "type", "trusted")
        ));
    }
}
