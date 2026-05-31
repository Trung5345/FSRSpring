package com.fsrspring.vocab.controller;

import com.fsrspring.vocab.model.QuizSession;
import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.service.QuizService;
import com.fsrspring.vocab.service.WordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizControllerTests {

    @Mock
    private QuizService quizService;

    @Mock
    private WordService wordService;

    private QuizController controller;
    private Word sampleWord;
    private QuizSession sampleSession;

    @BeforeEach
    void setUp() {
        controller = new QuizController(quizService, wordService);
        sampleWord = Word.builder()
                .id(1L)
                .word("hello")
                .translation("xin chào")
                .build();
        sampleSession = QuizSession.builder()
                .id(1L)
                .totalQuestions(10)
                .correctAnswers(7)
                .incorrectAnswers(3)
                .build();
    }

    @Test
    void startQuiz_noFilters_returnsSessionWithWords() {
        when(wordService.getRandomWords(anyInt())).thenReturn(List.of(sampleWord));
        when(quizService.startSession(anyInt(), isNull(), isNull())).thenReturn(sampleSession);

        ResponseEntity<Map<String, Object>> response = controller.startQuiz(10, null, null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).containsKeys("sessionId", "words", "totalQuestions", "distractors");
        assertThat(body).containsEntry("sessionId", 1L);
    }

    @Test
    void startQuiz_withCategory_callsGetWordsByCategory() {
        when(wordService.getWordsByCategory("Business")).thenReturn(List.of(sampleWord));
        when(quizService.startSession(anyInt(), eq("Business"), isNull())).thenReturn(sampleSession);

        ResponseEntity<Map<String, Object>> response = controller.startQuiz(10, "Business", null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(wordService).getWordsByCategory("Business");
    }

    @Test
    void startQuiz_withDifficulty_callsGetWordsByDifficulty() {
        when(wordService.getWordsByDifficulty(Word.DifficultyLevel.BEGINNER)).thenReturn(List.of(sampleWord));
        when(quizService.startSession(anyInt(), isNull(), eq(Word.DifficultyLevel.BEGINNER))).thenReturn(sampleSession);

        ResponseEntity<Map<String, Object>> response = controller.startQuiz(10, null, Word.DifficultyLevel.BEGINNER, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(wordService).getWordsByDifficulty(Word.DifficultyLevel.BEGINNER);
    }

    @Test
    void startQuiz_wordsFilteredWhenNoTranslation_excludesWordsWithoutTranslation() {
        Word wordWithoutTranslation = Word.builder().id(2L).word("world").translation("").build();
        when(wordService.getRandomWords(anyInt())).thenReturn(List.of(sampleWord, wordWithoutTranslation));
        when(quizService.startSession(anyInt(), isNull(), isNull())).thenReturn(sampleSession);

        ResponseEntity<Map<String, Object>> response = controller.startQuiz(10, null, null, null, null, "en-vi");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void startQuiz_emptyWordPool_returnsSessionWithZeroWords() {
        when(wordService.getRandomWords(anyInt())).thenReturn(List.of());
        when(quizService.startSession(eq(0), isNull(), isNull())).thenReturn(sampleSession);

        ResponseEntity<Map<String, Object>> response = controller.startQuiz(10, null, null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void submitAnswer_correctAnswer_returnsUpdatedSession() {
        when(quizService.submitAnswer(1L, 1L, true)).thenReturn(sampleSession);

        ResponseEntity<QuizSession> response = controller.submitAnswer(1L, 1L, true);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(sampleSession);
        verify(quizService).submitAnswer(1L, 1L, true);
    }

    @Test
    void submitAnswer_incorrectAnswer_returnsUpdatedSession() {
        when(quizService.submitAnswer(1L, 1L, false)).thenReturn(sampleSession);

        ResponseEntity<QuizSession> response = controller.submitAnswer(1L, 1L, false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(quizService).submitAnswer(1L, 1L, false);
    }

    @Test
    void completeSession_returnsCompletedSession() {
        when(quizService.completeSession(1L)).thenReturn(sampleSession);

        ResponseEntity<QuizSession> response = controller.completeSession(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(sampleSession);
        verify(quizService).completeSession(1L);
    }

    @Test
    void getRecentSessions_returnsList() {
        when(quizService.getRecentSessions()).thenReturn(List.of(sampleSession));

        ResponseEntity<List<QuizSession>> response = controller.getRecentSessions();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getRecentSessions_emptyHistory_returnsEmptyList() {
        when(quizService.getRecentSessions()).thenReturn(List.of());

        ResponseEntity<List<QuizSession>> response = controller.getRecentSessions();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getQuizStats_returnsStatsWithCompletedSessionsAndAverageScore() {
        when(quizService.getAverageScore()).thenReturn(75.5);
        when(quizService.countCompletedSessions()).thenReturn(10L);

        ResponseEntity<Map<String, Object>> response = controller.getQuizStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("completedSessions", 10L);
        assertThat(response.getBody()).containsEntry("averageScore", 75.5);
    }

    @Test
    void getQuizStats_nullAverageScore_returnsZeroAsDefault() {
        when(quizService.getAverageScore()).thenReturn(null);
        when(quizService.countCompletedSessions()).thenReturn(0L);

        ResponseEntity<Map<String, Object>> response = controller.getQuizStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("averageScore", 0.0);
    }

    @Test
    void getQuizStats_scoreIsRoundedToOneDecimal() {
        when(quizService.getAverageScore()).thenReturn(75.55);
        when(quizService.countCompletedSessions()).thenReturn(5L);

        ResponseEntity<Map<String, Object>> response = controller.getQuizStats();

        assertThat(response.getBody()).containsEntry("averageScore", 75.6);
    }
}
