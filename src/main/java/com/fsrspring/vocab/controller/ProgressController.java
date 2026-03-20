package com.fsrspring.vocab.controller;

import com.fsrspring.vocab.model.UserProgress;
import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.service.FsrsService;
import com.fsrspring.vocab.service.ProgressService;
import com.fsrspring.vocab.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;
    private final WordService wordService;
    private final FsrsService fsrsService;

    @GetMapping
    public ResponseEntity<List<UserProgress>> getAllProgress() {
        return ResponseEntity.ok(progressService.getAllProgress());
    }

    @GetMapping("/word/{wordId}")
    public ResponseEntity<UserProgress> getProgressByWord(@PathVariable Long wordId) {
        UserProgress progress = progressService.getProgressByWordId(wordId);
        if (progress == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(progress);
    }

    @PostMapping("/word/{wordId}/answer")
    public ResponseEntity<UserProgress> recordAnswer(
            @PathVariable Long wordId,
            @RequestParam boolean correct) {
        Word word = wordService.getWordById(wordId);
        UserProgress progress = progressService.recordAnswer(word, correct);
        return ResponseEntity.ok(progress);
    }

    @GetMapping("/review")
    public ResponseEntity<List<UserProgress>> getWordsForReview() {
        return ResponseEntity.ok(progressService.getWordsForReview());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long mastered = progressService.countMastered();
        long learning = progressService.countLearning();
        long totalCorrect = progressService.totalCorrect();
        long totalIncorrect = progressService.totalIncorrect();
        long totalAttempts = totalCorrect + totalIncorrect;
        double accuracy = totalAttempts > 0 ? (double) totalCorrect / totalAttempts * 100 : 0.0;

        return ResponseEntity.ok(Map.of(
                "mastered", mastered,
                "learning", learning,
                "totalCorrect", totalCorrect,
                "totalIncorrect", totalIncorrect,
            "dueNow", fsrsService.getFsrsStats().get("dueNow"),
                "accuracy", Math.round(accuracy * 10.0) / 10.0
        ));
    }
}
