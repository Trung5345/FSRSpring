package com.fsrspring.vocab.controller;

import com.fsrspring.vocab.model.UserProgress;
import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.service.FsrsService;
import com.fsrspring.vocab.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fsrs")
@RequiredArgsConstructor
public class FsrsController {

    private final FsrsService fsrsService;
    private final WordService wordService;

    @GetMapping("/due")
    public ResponseEntity<List<UserProgress>> getDueWords(@RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(fsrsService.getDueWords(limit));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getFsrsStats() {
        return ResponseEntity.ok(fsrsService.getFsrsStats());
    }

    @PostMapping("/review")
    public ResponseEntity<UserProgress> review(
            @RequestParam Long wordId,
            @RequestParam int rating,
            @RequestParam(defaultValue = "0") long responseMs) {
        Word word = wordService.getWordById(wordId);
        return ResponseEntity.ok(fsrsService.reviewWord(word, rating, responseMs));
    }
}
