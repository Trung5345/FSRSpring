package com.fsrspring.vocab.controller;

import com.fsrspring.vocab.model.UserProgress;
import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.service.FsrsService;
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
class FsrsControllerTests {

    @Mock
    private FsrsService fsrsService;

    @Mock
    private WordService wordService;

    private FsrsController controller;
    private Word sampleWord;
    private UserProgress sampleProgress;

    @BeforeEach
    void setUp() {
        controller = new FsrsController(fsrsService, wordService);
        sampleWord = Word.builder()
                .id(1L)
                .word("hello")
                .translation("xin chào")
                .difficulty(Word.DifficultyLevel.BEGINNER)
                .build();
        sampleProgress = UserProgress.builder()
                .id(1L)
                .word(sampleWord)
                .correctCount(3)
                .incorrectCount(1)
                .mastery(UserProgress.MasteryLevel.REVIEWING)
                .build();
    }

    @Test
    void getDueWords_defaultLimit_returnsDueList() {
        when(fsrsService.getDueWords(20)).thenReturn(List.of(sampleProgress));

        ResponseEntity<List<UserProgress>> response = controller.getDueWords(20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getWord().getWord()).isEqualTo("hello");
    }

    @Test
    void getDueWords_emptyQueue_returnsEmptyList() {
        when(fsrsService.getDueWords(20)).thenReturn(List.of());

        ResponseEntity<List<UserProgress>> response = controller.getDueWords(20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getDueWords_customLimit_passesLimitToService() {
        when(fsrsService.getDueWords(5)).thenReturn(List.of());

        controller.getDueWords(5);

        verify(fsrsService).getDueWords(5);
    }

    @Test
    void getFsrsStats_returnsStatsMap() {
        Map<String, Object> stats = Map.of(
                "dueNow", 5L,
                "totalWords", 100L,
                "mastered", 30L
        );
        when(fsrsService.getFsrsStats()).thenReturn(stats);

        ResponseEntity<Map<String, Object>> response = controller.getFsrsStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKeys("dueNow", "totalWords", "mastered");
        assertThat(response.getBody()).containsEntry("dueNow", 5L);
    }

    @Test
    void review_ratingGood_returnsUpdatedProgress() {
        when(wordService.getWordById(1L)).thenReturn(sampleWord);
        when(fsrsService.reviewWord(sampleWord, 3, 0L)).thenReturn(sampleProgress);

        ResponseEntity<UserProgress> response = controller.review(1L, 3, 0L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(sampleProgress);
        verify(wordService).getWordById(1L);
        verify(fsrsService).reviewWord(sampleWord, 3, 0L);
    }

    @Test
    void review_ratingAgain_returnsProgressWithLapse() {
        UserProgress lapseProgress = UserProgress.builder()
                .id(1L)
                .word(sampleWord)
                .correctCount(3)
                .incorrectCount(2)
                .mastery(UserProgress.MasteryLevel.REVIEWING)
                .build();
        when(wordService.getWordById(1L)).thenReturn(sampleWord);
        when(fsrsService.reviewWord(sampleWord, 1, 0L)).thenReturn(lapseProgress);

        ResponseEntity<UserProgress> response = controller.review(1L, 1, 0L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getIncorrectCount()).isEqualTo(2);
    }

    @Test
    void review_withResponseTime_passesResponseTimeToService() {
        when(wordService.getWordById(1L)).thenReturn(sampleWord);
        when(fsrsService.reviewWord(sampleWord, 3, 1500L)).thenReturn(sampleProgress);

        controller.review(1L, 3, 1500L);

        verify(fsrsService).reviewWord(sampleWord, 3, 1500L);
    }
}
