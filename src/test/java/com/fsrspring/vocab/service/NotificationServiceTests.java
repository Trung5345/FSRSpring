package com.fsrspring.vocab.service;

import com.fsrspring.vocab.model.AppNotification;
import com.fsrspring.vocab.model.AppUser;
import com.fsrspring.vocab.model.UserProgress;
import com.fsrspring.vocab.repository.AppNotificationRepository;
import com.fsrspring.vocab.repository.UserProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTests {

    @Mock
    private AppNotificationRepository notificationRepository;

    @Mock
    private UserProgressRepository progressRepository;

    @Mock
    private JavaMailSender mailSender;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, progressRepository, mailSender);
        ReflectionTestUtils.setField(notificationService, "defaultReminderEmail", "fallback@example.com");
    }

    @Test
    void dispatchReviewRemindersCreatesUserScopedNotificationsWithoutSecurityContext() {
        AppUser alice = AppUser.builder().id(1L).email("alice@example.com").build();
        AppUser bob = AppUser.builder().id(2L).email("bob@example.com").build();

        when(progressRepository.findDueWordsForAllUsers(any(LocalDateTime.class))).thenReturn(List.of(
                UserProgress.builder().user(alice).build(),
                UserProgress.builder().user(alice).build(),
                UserProgress.builder().user(bob).build()
        ));

        notificationService.dispatchReviewReminders();

        ArgumentCaptor<AppNotification> notificationCaptor = ArgumentCaptor.forClass(AppNotification.class);
        verify(notificationRepository, org.mockito.Mockito.times(2)).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getAllValues())
                .extracting(notification -> notification.getUser().getEmail(), AppNotification::getMessage)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("alice@example.com", "You have 2 vocabulary cards due for review."),
                        org.assertj.core.groups.Tuple.tuple("bob@example.com", "You have 1 vocabulary cards due for review.")
                );

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, org.mockito.Mockito.times(2)).send(mailCaptor.capture());
        assertThat(mailCaptor.getAllValues())
                .extracting(message -> message.getTo()[0], SimpleMailMessage::getText)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("alice@example.com", "You have 2 vocabulary cards due for review.\n\nOpen: http://localhost:8080/flashcards"),
                        org.assertj.core.groups.Tuple.tuple("bob@example.com", "You have 1 vocabulary cards due for review.\n\nOpen: http://localhost:8080/flashcards")
                );
    }
}
