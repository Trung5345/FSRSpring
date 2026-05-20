package com.fsrspring.vocab.service;

import com.fsrspring.vocab.model.QuizSession;
import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.repository.QuizSessionRepository;
import com.fsrspring.vocab.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizService {

    private final QuizSessionRepository quizSessionRepository;
    private final WordService wordService;
    private final ProgressService progressService;
    private final CurrentUserService currentUserService;
    private final StreakService streakService;

    public QuizSession startSession(int questionCount, String category, Word.DifficultyLevel difficulty) {
        QuizSession session = QuizSession.builder()
                .user(currentUserService.getCurrentUserOptional().orElse(null))
                .totalQuestions(questionCount)
                .category(category)
                .difficulty(difficulty)
                .build();
        return quizSessionRepository.save(session);
    }

    public QuizSession submitAnswer(Long sessionId, Long wordId, boolean correct) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz session not found: " + sessionId));
        Word word = wordService.getWordById(wordId);
        currentUserService.getCurrentUserOptional().ifPresent(user -> progressService.recordAnswer(word, correct));
        if (correct) {
            session.setCorrectAnswers(session.getCorrectAnswers() + 1);
        } else {
            session.setIncorrectAnswers(session.getIncorrectAnswers() + 1);
        }
        return quizSessionRepository.save(session);
    }

    public QuizSession completeSession(Long sessionId) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz session not found: " + sessionId));
        session.setCompletedAt(LocalDateTime.now());
        
        currentUserService.getCurrentUserOptional().ifPresent(user -> streakService.checkIn());

        return quizSessionRepository.save(session);
    }

    public List<QuizSession> getRecentSessions() {
        return quizSessionRepository.findTop10ByOrderByStartedAtDesc();
    }

    public Double getAverageScore() {
        return quizSessionRepository.findAverageScore();
    }

    public long countCompletedSessions() {
        return quizSessionRepository.countCompleted();
    }
}
