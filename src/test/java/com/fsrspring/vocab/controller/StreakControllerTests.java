package com.fsrspring.vocab.controller;

import com.fsrspring.vocab.model.UserStreak;
import com.fsrspring.vocab.service.StreakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreakControllerTests {

    @Mock
    private StreakService streakService;

    private StreakController controller;
    private UserStreak sampleStreak;

    @BeforeEach
    void setUp() {
        controller = new StreakController(streakService);
        sampleStreak = UserStreak.builder()
                .id(1L)
                .currentStreak(7)
                .longestStreak(30)
                .totalDaysStudied(45)
                .lastStudyDate(LocalDate.now())
                .build();
    }

    @Test
    void getStreak_returnsCurrentStreak() {
        when(streakService.getStreak()).thenReturn(sampleStreak);

        ResponseEntity<UserStreak> response = controller.getStreak();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(sampleStreak);
        assertThat(response.getBody().getCurrentStreak()).isEqualTo(7);
        assertThat(response.getBody().getLongestStreak()).isEqualTo(30);
        assertThat(response.getBody().getTotalDaysStudied()).isEqualTo(45);
    }

    @Test
    void getStreak_newUser_returnsZeroStreak() {
        UserStreak newStreak = UserStreak.builder()
                .id(2L)
                .currentStreak(0)
                .longestStreak(0)
                .totalDaysStudied(0)
                .build();
        when(streakService.getStreak()).thenReturn(newStreak);

        ResponseEntity<UserStreak> response = controller.getStreak();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getCurrentStreak()).isEqualTo(0);
    }

    @Test
    void checkIn_incrementsStreakAndReturnsUpdated() {
        UserStreak updatedStreak = UserStreak.builder()
                .id(1L)
                .currentStreak(8)
                .longestStreak(30)
                .totalDaysStudied(46)
                .lastStudyDate(LocalDate.now())
                .build();
        when(streakService.checkIn()).thenReturn(updatedStreak);

        ResponseEntity<UserStreak> response = controller.checkIn();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getCurrentStreak()).isEqualTo(8);
        assertThat(response.getBody().getTotalDaysStudied()).isEqualTo(46);
        verify(streakService).checkIn();
    }

    @Test
    void checkIn_breaksStreak_resetsToOne() {
        UserStreak resetStreak = UserStreak.builder()
                .id(1L)
                .currentStreak(1)
                .longestStreak(30)
                .totalDaysStudied(46)
                .lastStudyDate(LocalDate.now())
                .build();
        when(streakService.checkIn()).thenReturn(resetStreak);

        ResponseEntity<UserStreak> response = controller.checkIn();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getCurrentStreak()).isEqualTo(1);
    }
}
