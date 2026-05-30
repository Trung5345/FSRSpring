package com.fsrspring.vocab.service;

import com.fsrspring.vocab.dto.ReminderSettingsRequest;
import com.fsrspring.vocab.dto.ReminderSettingsResponse;
import com.fsrspring.vocab.model.AppNotification;
import com.fsrspring.vocab.model.AppUser;
import com.fsrspring.vocab.repository.AppNotificationRepository;
import com.fsrspring.vocab.repository.AppUserRepository;
import com.fsrspring.vocab.repository.ReviewEventRepository;
import com.fsrspring.vocab.repository.UserProgressRepository;
import com.fsrspring.vocab.security.CurrentUserService;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTests {

    @Mock
    private AppNotificationRepository notificationRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private UserProgressRepository progressRepository;

    @Mock
    private ReviewEventRepository reviewEventRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private JavaMailSender mailSender;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                appUserRepository,
                progressRepository,
                reviewEventRepository,
                currentUserService,
                mailSender
        );
        ReflectionTestUtils.setField(notificationService, "defaultReminderEmail", "fallback@example.com");
        ReflectionTestUtils.setField(notificationService, "eveningRemainingThreshold", 10);
    }

    @Test
    void dispatchReviewRemindersCreatesUserScopedNotificationsWithoutSecurityContext() {
        LocalDateTime reminderTime = LocalDateTime.of(2026, 5, 21, 20, 0);
        AppUser alice = learner(1L, "alice@example.com", reminderTime);
        AppUser bob = learner(2L, "bob@example.com", reminderTime);

        when(appUserRepository.findAll()).thenReturn(List.of(alice, bob));
        when(progressRepository.countDueWords(eq(alice), any(LocalDateTime.class))).thenReturn(2L);
        when(progressRepository.countDueWords(eq(bob), any(LocalDateTime.class))).thenReturn(1L);

        notificationService.dispatchReviewReminders(reminderTime);

        ArgumentCaptor<AppNotification> notificationCaptor = ArgumentCaptor.forClass(AppNotification.class);
        verify(notificationRepository, org.mockito.Mockito.times(2)).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getAllValues())
                .extracting(notification -> notification.getUser().getEmail(), AppNotification::getType, AppNotification::getMessage)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("alice@example.com", AppNotification.NotificationType.DAILY_DUE_REMINDER, "Bạn có 2 thẻ cần ôn hôm nay."),
                        org.assertj.core.groups.Tuple.tuple("bob@example.com", AppNotification.NotificationType.DAILY_DUE_REMINDER, "Bạn có 1 thẻ cần ôn hôm nay.")
                );

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, org.mockito.Mockito.times(2)).send(mailCaptor.capture());
        assertThat(mailCaptor.getAllValues())
                .extracting(message -> message.getTo()[0], SimpleMailMessage::getText)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("alice@example.com", "Bạn có 2 thẻ cần ôn hôm nay.\n\nOpen: http://localhost:8080/flashcards"),
                        org.assertj.core.groups.Tuple.tuple("bob@example.com", "Bạn có 1 thẻ cần ôn hôm nay.\n\nOpen: http://localhost:8080/flashcards")
                );
    }

    @Test
    void dispatchReviewRemindersCreatesEveningFallbackOnlyWhenDueCardsRemain() {
        LocalDateTime reminderTime = LocalDateTime.of(2026, 5, 21, 21, 30);
        AppUser alice = learner(1L, "alice@example.com", reminderTime);

        when(appUserRepository.findAll()).thenReturn(List.of(alice));
        when(progressRepository.countDueWords(eq(alice), any(LocalDateTime.class))).thenReturn(2L);
        when(reviewEventRepository.existsByUserAndReviewedAtBetween(eq(alice), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

        notificationService.dispatchReviewReminders(reminderTime);

        ArgumentCaptor<AppNotification> notificationCaptor = ArgumentCaptor.forClass(AppNotification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getType()).isEqualTo(AppNotification.NotificationType.EVENING_REVIEW_REMINDER);
        assertThat(notificationCaptor.getValue().getMessage()).isEqualTo("Bạn còn 2 thẻ chưa ôn hôm nay.");
    }

    @Test
    void dispatchReviewRemindersUsesComebackCadenceForInactiveOverdueCards() {
        LocalDateTime reminderTime = LocalDateTime.of(2026, 5, 21, 20, 0);
        AppUser alice = learner(1L, "alice@example.com", LocalDateTime.of(2026, 5, 18, 8, 0));

        when(appUserRepository.findAll()).thenReturn(List.of(alice));
        when(progressRepository.countDueWords(eq(alice), any(LocalDateTime.class))).thenReturn(2L);
        when(progressRepository.countOverdueWords(eq(alice), any(LocalDateTime.class))).thenReturn(2L);

        notificationService.dispatchReviewReminders(reminderTime);

        ArgumentCaptor<AppNotification> notificationCaptor = ArgumentCaptor.forClass(AppNotification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getType()).isEqualTo(AppNotification.NotificationType.OVERDUE_REMINDER);
        assertThat(notificationCaptor.getValue().getMessage()).isEqualTo("Bạn có 2 thẻ quá hạn. Ôn 10 phút để giữ nhịp nhé.");
    }

    @Test
    void dispatchReviewRemindersDoesNotQueryDueCountsOutsideConfiguredReminderMinutes() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 21, 19, 59);
        AppUser alice = learner(1L, "alice@example.com", now);

        when(appUserRepository.findAll()).thenReturn(List.of(alice));

        notificationService.dispatchReviewReminders(now);

        verify(progressRepository, never()).countDueWords(any(AppUser.class), any(LocalDateTime.class));
        verify(progressRepository, never()).countOverdueWords(any(AppUser.class), any(LocalDateTime.class));
        verify(notificationRepository, never()).save(any(AppNotification.class));
        verifyNoInteractions(mailSender);
    }

    @Test
    void dispatchReviewRemindersDoesNothingWhenDailyDueCountIsZero() {
        LocalDateTime reminderTime = LocalDateTime.of(2026, 5, 21, 20, 0);
        AppUser alice = learner(1L, "alice@example.com", reminderTime);

        when(appUserRepository.findAll()).thenReturn(List.of(alice));
        when(progressRepository.countDueWords(eq(alice), any(LocalDateTime.class))).thenReturn(0L);

        notificationService.dispatchReviewReminders(reminderTime);

        verify(notificationRepository, never()).save(any(AppNotification.class));
        verifyNoInteractions(mailSender);
    }

    @Test
    void dispatchReviewRemindersSuppressesDuplicateDailyReminderForSameUserAndDay() {
        LocalDateTime reminderTime = LocalDateTime.of(2026, 5, 21, 20, 0);
        AppUser alice = learner(1L, "alice@example.com", reminderTime);

        when(appUserRepository.findAll()).thenReturn(List.of(alice));
        when(progressRepository.countDueWords(eq(alice), any(LocalDateTime.class))).thenReturn(12L);
        when(notificationRepository.existsByUserAndTypeAndScheduledAtBetween(
                eq(alice),
                eq(AppNotification.NotificationType.DAILY_DUE_REMINDER),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(true);

        notificationService.dispatchReviewReminders(reminderTime);

        verify(notificationRepository, never()).save(any(AppNotification.class));
        verifyNoInteractions(mailSender);
    }

    @Test
    void dispatchReviewRemindersSkipsAllReminderWorkWhenUserDisabledReminders() {
        LocalDateTime reminderTime = LocalDateTime.of(2026, 5, 21, 20, 0);
        AppUser alice = learner(1L, "alice@example.com", reminderTime);
        alice.setReviewRemindersEnabled(false);

        when(appUserRepository.findAll()).thenReturn(List.of(alice));

        notificationService.dispatchReviewReminders(reminderTime);

        verify(progressRepository, never()).countDueWords(any(AppUser.class), any(LocalDateTime.class));
        verify(notificationRepository, never()).save(any(AppNotification.class));
        verifyNoInteractions(mailSender);
    }

    @Test
    void dispatchReviewRemindersSkipsEveningFallbackWhenUserAlreadyStudiedAndFewCardsRemain() {
        LocalDateTime reminderTime = LocalDateTime.of(2026, 5, 21, 21, 30);
        AppUser alice = learner(1L, "alice@example.com", reminderTime);

        when(appUserRepository.findAll()).thenReturn(List.of(alice));
        when(progressRepository.countDueWords(eq(alice), any(LocalDateTime.class))).thenReturn(9L);
        when(reviewEventRepository.existsByUserAndReviewedAtBetween(eq(alice), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

        notificationService.dispatchReviewReminders(reminderTime);

        verify(notificationRepository, never()).save(any(AppNotification.class));
        verifyNoInteractions(mailSender);
    }

    @Test
    void dispatchReviewRemindersStillSendsEveningFallbackWhenUserStudiedButManyCardsRemain() {
        LocalDateTime reminderTime = LocalDateTime.of(2026, 5, 21, 21, 30);
        AppUser alice = learner(1L, "alice@example.com", reminderTime);

        when(appUserRepository.findAll()).thenReturn(List.of(alice));
        when(progressRepository.countDueWords(eq(alice), any(LocalDateTime.class))).thenReturn(10L);
        when(reviewEventRepository.existsByUserAndReviewedAtBetween(eq(alice), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

        notificationService.dispatchReviewReminders(reminderTime);

        ArgumentCaptor<AppNotification> notificationCaptor = ArgumentCaptor.forClass(AppNotification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getType()).isEqualTo(AppNotification.NotificationType.EVENING_REVIEW_REMINDER);
        assertThat(notificationCaptor.getValue().getMessage()).isEqualTo("Bạn còn 10 thẻ chưa ôn hôm nay.");
    }

    @Test
    void dispatchReviewRemindersSuppressesDuplicateEveningFallbackForSameUserAndDay() {
        LocalDateTime reminderTime = LocalDateTime.of(2026, 5, 21, 21, 30);
        AppUser alice = learner(1L, "alice@example.com", reminderTime);

        when(appUserRepository.findAll()).thenReturn(List.of(alice));
        when(progressRepository.countDueWords(eq(alice), any(LocalDateTime.class))).thenReturn(12L);
        when(notificationRepository.existsByUserAndTypeAndScheduledAtBetween(
                eq(alice),
                eq(AppNotification.NotificationType.EVENING_REVIEW_REMINDER),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(true);

        notificationService.dispatchReviewReminders(reminderTime);

        verify(notificationRepository, never()).save(any(AppNotification.class));
        verifyNoInteractions(mailSender);
    }

    @Test
    void dispatchReviewRemindersSuppressesComebackReminderInsideCadenceWindow() {
        LocalDateTime reminderTime = LocalDateTime.of(2026, 5, 21, 20, 0);
        AppUser alice = learner(1L, "alice@example.com", LocalDateTime.of(2026, 5, 18, 8, 0));

        when(appUserRepository.findAll()).thenReturn(List.of(alice));
        when(progressRepository.countDueWords(eq(alice), any(LocalDateTime.class))).thenReturn(63L);
        when(progressRepository.countOverdueWords(eq(alice), any(LocalDateTime.class))).thenReturn(63L);
        when(notificationRepository.existsByUserAndTypeAndScheduledAtAfter(
                eq(alice),
                eq(AppNotification.NotificationType.OVERDUE_REMINDER),
                any(LocalDateTime.class)
        )).thenReturn(true);

        notificationService.dispatchReviewReminders(reminderTime);

        verify(notificationRepository, never()).save(any(AppNotification.class));
        verifyNoInteractions(mailSender);
    }

    @Test
    void dispatchReviewRemindersFallsBackToDailyReminderForInactiveUserWithoutOverdueCards() {
        LocalDateTime reminderTime = LocalDateTime.of(2026, 5, 21, 20, 0);
        AppUser alice = learner(1L, "alice@example.com", LocalDateTime.of(2026, 5, 18, 8, 0));

        when(appUserRepository.findAll()).thenReturn(List.of(alice));
        when(progressRepository.countDueWords(eq(alice), any(LocalDateTime.class))).thenReturn(4L);
        when(progressRepository.countOverdueWords(eq(alice), any(LocalDateTime.class))).thenReturn(0L);

        notificationService.dispatchReviewReminders(reminderTime);

        ArgumentCaptor<AppNotification> notificationCaptor = ArgumentCaptor.forClass(AppNotification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getType()).isEqualTo(AppNotification.NotificationType.DAILY_DUE_REMINDER);
        assertThat(notificationCaptor.getValue().getMessage()).isEqualTo("Bạn có 4 thẻ cần ôn hôm nay.");
    }

    @Test
    void listLatestAndUnreadCountAreScopedToCurrentUser() {
        AppUser alice = learner(1L, "alice@example.com", LocalDateTime.of(2026, 5, 21, 8, 0));
        AppNotification notification = AppNotification.builder()
                .user(alice)
                .title("Đến giờ ôn tập")
                .message("Bạn có 2 thẻ cần ôn hôm nay.")
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(alice);
        when(notificationRepository.findTop50ByUserOrderByCreatedAtDesc(alice)).thenReturn(List.of(notification));
        when(notificationRepository.countByUserAndIsReadFalse(alice)).thenReturn(1L);

        assertThat(notificationService.listLatest()).containsExactly(notification);
        assertThat(notificationService.unreadCount()).isEqualTo(1L);
    }

    @Test
    void markAsReadUsesCurrentUserScope() {
        AppUser alice = learner(1L, "alice@example.com", LocalDateTime.of(2026, 5, 21, 8, 0));
        AppNotification notification = AppNotification.builder()
                .id(10L)
                .user(alice)
                .title("Đến giờ ôn tập")
                .message("Bạn có 2 thẻ cần ôn hôm nay.")
                .isRead(false)
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(alice);
        when(notificationRepository.findByIdAndUser(10L, alice)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        AppNotification updated = notificationService.markAsRead(10L);

        assertThat(updated.getIsRead()).isTrue();
        verify(notificationRepository).findByIdAndUser(10L, alice);
    }

    @Test
    void updateReminderSettingsPersistsNormalizedUserPreferences() {
        AppUser alice = learner(1L, "alice@example.com", LocalDateTime.of(2026, 5, 21, 8, 0));
        ReminderSettingsRequest request = new ReminderSettingsRequest(
                true,
                "08:15",
                false,
                "21:15",
                true,
                99,
                null
        );

        when(currentUserService.getCurrentUser()).thenReturn(alice);
        when(appUserRepository.save(alice)).thenReturn(alice);

        ReminderSettingsResponse response = notificationService.updateReminderSettings(request);

        assertThat(response.reviewRemindersEnabled()).isTrue();
        assertThat(response.preferredReminderTime()).isEqualTo("08:15");
        assertThat(response.eveningReminderEnabled()).isFalse();
        assertThat(response.eveningReminderTime()).isEqualTo("21:15");
        assertThat(response.comebackReminderEnabled()).isTrue();
        assertThat(response.comebackReminderIntervalDays()).isEqualTo(3);
        verify(appUserRepository).save(alice);
    }

    @Test
    void updateReminderSettingsRejectsInvalidReminderTime() {
        AppUser alice = learner(1L, "alice@example.com", LocalDateTime.of(2026, 5, 21, 8, 0));
        ReminderSettingsRequest request = new ReminderSettingsRequest(
                true,
                "25:99",
                true,
                "21:30",
                true,
                3,
                null
        );

        when(currentUserService.getCurrentUser()).thenReturn(alice);

        assertThatThrownBy(() -> notificationService.updateReminderSettings(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid reminder time: 25:99");
        verify(appUserRepository, never()).save(any(AppUser.class));
    }

    private AppUser learner(Long id, String email, LocalDateTime createdAt) {
        return AppUser.builder()
                .id(id)
                .email(email)
                .createdAt(createdAt)
                .preferredReminderTime(LocalTime.of(20, 0))
                .eveningReminderTime(LocalTime.of(21, 30))
                .build();
    }
}
