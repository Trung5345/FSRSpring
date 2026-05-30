package com.fsrspring.vocab.controller;

import com.fsrspring.vocab.dto.ReminderSettingsRequest;
import com.fsrspring.vocab.dto.ReminderSettingsResponse;
import com.fsrspring.vocab.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTests {

    @Mock
    private NotificationService notificationService;

    private NotificationController controller;

    @BeforeEach
    void setUp() {
        controller = new NotificationController(notificationService);
    }

    @Test
    void settingsReturnsCurrentReminderSettings() {
        ReminderSettingsResponse settings = new ReminderSettingsResponse(
                true,
                "20:00",
                true,
                "21:30",
                true,
                3,
                10
        );
        when(notificationService.getReminderSettings()).thenReturn(settings);

        ResponseEntity<ReminderSettingsResponse> response = controller.settings();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(settings);
    }

    @Test
    void updateSettingsReturnsPersistedReminderSettings() {
        ReminderSettingsRequest request = new ReminderSettingsRequest(
                true,
                "08:00",
                true,
                "21:00",
                true,
                2,
                null
        );
        ReminderSettingsResponse settings = new ReminderSettingsResponse(
                true,
                "08:00",
                true,
                "21:00",
                true,
                2,
                10
        );
        when(notificationService.updateReminderSettings(request)).thenReturn(settings);

        ResponseEntity<ReminderSettingsResponse> response = controller.updateSettings(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(settings);
        verify(notificationService).updateReminderSettings(request);
    }

    @Test
    void badRequestReturnsErrorMessage() {
        ResponseEntity<Map<String, String>> response = controller.badRequest(
                new IllegalArgumentException("Invalid reminder time: 25:99")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Invalid reminder time: 25:99");
    }
}
