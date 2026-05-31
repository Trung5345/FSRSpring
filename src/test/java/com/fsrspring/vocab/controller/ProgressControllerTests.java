package com.fsrspring.vocab.controller;

import com.fsrspring.vocab.model.UserProgress;
import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.service.FsrsService;
import com.fsrspring.vocab.service.ProgressService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgressControllerTests {

    @Mock
    private ProgressService progressService;

    @Mock
    private WordService wordService;

    @Mock
    private FsrsService fsrsService;

    private ProgressController controller;
    private Word sampleWord;
    private UserProgress sampleProgress;

    @BeforeEach
    void setUp() {
        controller = new ProgressController(progressService, wordService, fsrsService);
        sampleWord = Word.builder()
                .id(1L)
                .word("hello")
                .translation("xin chào")
                .build();
        sampleProgress = UserProgress.builder()
                .id(1L)
                .word(sampleWord)
                .correctCount(5)
                .incorrectCount(1)
                .mastery(UserProgress.MasteryLevel.REVIEWING)
                .build();
    }

    @Test
    void getAllProgress_returnsProgressList() {
        when(progressService.getAllProgress()).thenReturn(List.of(sampleProgress));

        ResponseEntity<List<UserProgress>> response = controller.getAllProgress();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getAllProgress_emptyList_returnsEmpty() {
        when(progressService.getAllProgress()).thenReturn(List.of());

        ResponseEntity<List<UserProgress>> response = controller.getAllProgress();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getProgressByWord_existingWord_returnsProgress() {
        when(progressService.getProgressByWordId(1L)).thenReturn(sampleProgress);

        ResponseEntity<UserProgress> response = controller.getProgressByWord(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(sampleProgress);
    }

    @Test
    void getProgressByWord_noProgress_returnsNotFound() {
        when(progressService.getProgressByWordId(99L)).thenReturn(null);

        ResponseEntity<UserProgress> response = controller.getProgressByWord(99L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void recordAnswer_correctAnswer_returnsUpdatedProgress() {
        when(wordService.getWordById(1L)).thenReturn(sampleWord);
        when(progressService.recordAnswer(sampleWord, true)).thenReturn(sampleProgress);

        ResponseEntity<UserProgress> response = controller.recordAnswer(1L, true);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(sampleProgress);
        verify(progressService).recordAnswer(sampleWord, true);
    }

    @Test
    void recordAnswer_incorrectAnswer_callsServiceWithFalse() {
        when(wordService.getWordById(1L)).thenReturn(sampleWord);
        when(progressService.recordAnswer(sampleWord, false)).thenReturn(sampleProgress);

        controller.recordAnswer(1L, false);

        verify(progressService).recordAnswer(sampleWord, false);
    }

    @Test
    void getWordsForReview_returnsReviewList() {
        when(progressService.getWordsForReview()).thenReturn(List.of(sampleProgress));

        ResponseEntity<List<UserProgress>> response = controller.getWordsForReview();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getStats_returnsAllStatsFields() {
        when(progressService.countMastered()).thenReturn(10L);
        when(progressService.countLearning()).thenReturn(5L);
        when(progressService.totalCorrect()).thenReturn(80L);
        when(progressService.totalIncorrect()).thenReturn(20L);
        when(fsrsService.getFsrsStats()).thenReturn(Map.of("dueNow", 3L));

        ResponseEntity<Map<String, Object>> response = controller.getStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).containsKeys("mastered", "learning", "totalCorrect", "totalIncorrect", "accuracy", "dueNow");
        assertThat(body).containsEntry("mastered", 10L);
        assertThat(body).containsEntry("learning", 5L);
        assertThat(body).containsEntry("totalCorrect", 80L);
        assertThat(body).containsEntry("totalIncorrect", 20L);
        assertThat(body).containsEntry("dueNow", 3L);
    }

    @Test
    void getStats_withAttempts_calculatesAccuracyCorrectly() {
        when(progressService.countMastered()).thenReturn(0L);
        when(progressService.countLearning()).thenReturn(0L);
        when(progressService.totalCorrect()).thenReturn(75L);
        when(progressService.totalIncorrect()).thenReturn(25L);
        when(fsrsService.getFsrsStats()).thenReturn(Map.of("dueNow", 0L));

        ResponseEntity<Map<String, Object>> response = controller.getStats();

        assertThat(response.getBody()).containsEntry("accuracy", 75.0);
    }

    @Test
    void getStats_zeroAttempts_returnsZeroAccuracy() {
        when(progressService.countMastered()).thenReturn(0L);
        when(progressService.countLearning()).thenReturn(0L);
        when(progressService.totalCorrect()).thenReturn(0L);
        when(progressService.totalIncorrect()).thenReturn(0L);
        when(fsrsService.getFsrsStats()).thenReturn(Map.of("dueNow", 0L));

        ResponseEntity<Map<String, Object>> response = controller.getStats();

        assertThat(response.getBody()).containsEntry("accuracy", 0.0);
    }
}
