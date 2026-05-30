package com.fsrspring.vocab.controller;

import com.fsrspring.vocab.model.CefrLevel;
import com.fsrspring.vocab.model.QuizSession;
import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.service.QuizService;
import com.fsrspring.vocab.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final WordService wordService;

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startQuiz(
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Word.DifficultyLevel difficulty,
            @RequestParam(required = false) Long topicId,
            @RequestParam(required = false) CefrLevel cefrLevel,
            @RequestParam(required = false) String type) {
        List<Word> words;
        if (topicId != null && cefrLevel != null) {
            words = new ArrayList<>(wordService.getWordsByTopicAndCefr(topicId, cefrLevel));
        } else if (topicId != null) {
            words = new ArrayList<>(wordService.getWordsByTopic(topicId));
        } else if (cefrLevel != null) {
            words = new ArrayList<>(wordService.getWordsByCefrLevel(cefrLevel));
        } else if (category != null && difficulty != null) {
            words = new ArrayList<>(wordService.getWordsByCategoryAndDifficulty(category, difficulty));
        } else if (category != null) {
            words = new ArrayList<>(wordService.getWordsByCategory(category));
        } else if (difficulty != null) {
            words = new ArrayList<>(wordService.getWordsByDifficulty(difficulty));
        } else {
            // we request more initially to account for filtering down below
            words = new ArrayList<>(wordService.getRandomWords(count * 3));
        }
        
        // Filter out words based on quiz type requirements
        if ("en-vi".equals(type)) {
            words.removeIf(w -> w.getTranslation() == null || w.getTranslation().trim().isEmpty());
        } else if ("vi-en".equals(type)) {
            words.removeIf(w -> w.getWord() == null || w.getWord().trim().isEmpty());
        } else {
            // Default: Both must be present to be safe
            words.removeIf(w -> w.getWord() == null || w.getWord().trim().isEmpty() ||
                                w.getTranslation() == null || w.getTranslation().trim().isEmpty());
        }

        // Apply category/difficulty filter on top of topic/cefr if both are specified
        if (topicId != null || cefrLevel != null) {
            if (category != null) {
                final String cat = category;
                words.removeIf(w -> !cat.equals(w.getCategory()));
            }
            if (difficulty != null) {
                final Word.DifficultyLevel diff = difficulty;
                words.removeIf(w -> !diff.equals(w.getDifficulty()));
            }
        }
        
        Collections.shuffle(words);
        
        if (words.size() > count) {
            words = words.subList(0, count);
        }
        // Fetch random words to use as distractor answer options.
        // Exclude words already in the quiz session to avoid duplicates.
        final List<Long> sessionIds = words.stream().map(Word::getId).toList();
        List<Word> distractors = wordService.getRandomWords(20).stream()
                .filter(w -> !sessionIds.contains(w.getId()))
                .limit(15)
                .toList();
        QuizSession session = quizService.startSession(words.size(), category, difficulty);
        return ResponseEntity.ok(Map.of(
                "sessionId", session.getId(),
                "words", words,
                "totalQuestions", words.size(),
                "distractors", distractors
        ));
    }

    @PostMapping("/session/{sessionId}/answer")
    public ResponseEntity<QuizSession> submitAnswer(
            @PathVariable Long sessionId,
            @RequestParam Long wordId,
            @RequestParam boolean correct) {
        QuizSession session = quizService.submitAnswer(sessionId, wordId, correct);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/session/{sessionId}/complete")
    public ResponseEntity<QuizSession> completeSession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(quizService.completeSession(sessionId));
    }

    @GetMapping("/sessions/recent")
    public ResponseEntity<List<QuizSession>> getRecentSessions() {
        return ResponseEntity.ok(quizService.getRecentSessions());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getQuizStats() {
        Double avgScore = quizService.getAverageScore();
        long completed = quizService.countCompletedSessions();
        return ResponseEntity.ok(Map.of(
                "completedSessions", completed,
                "averageScore", avgScore != null ? Math.round(avgScore * 10.0) / 10.0 : 0.0
        ));
    }
}
