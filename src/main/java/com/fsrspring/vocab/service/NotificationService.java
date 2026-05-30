package com.fsrspring.vocab.service;

import com.fsrspring.vocab.dto.ReminderSettingsRequest;
import com.fsrspring.vocab.dto.ReminderSettingsResponse;
import com.fsrspring.vocab.model.AppNotification;
import com.fsrspring.vocab.model.AppUser;
import com.fsrspring.vocab.model.ReviewEvent;
import com.fsrspring.vocab.repository.AppUserRepository;
import com.fsrspring.vocab.repository.AppNotificationRepository;
import com.fsrspring.vocab.repository.ReviewEventRepository;
import com.fsrspring.vocab.repository.UserProgressRepository;
import com.fsrspring.vocab.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final AppNotificationRepository notificationRepository;
    private final AppUserRepository appUserRepository;
    private final UserProgressRepository progressRepository;
    private final ReviewEventRepository reviewEventRepository;
    private final CurrentUserService currentUserService;
    private final JavaMailSender mailSender;

    private static final LocalTime DEFAULT_DAILY_REMINDER_TIME = LocalTime.of(20, 0);
    private static final LocalTime DEFAULT_EVENING_REMINDER_TIME = LocalTime.of(21, 30);

    @Value("${app.reminder.default-email}")
    private String defaultReminderEmail;

    @Value("${app.reminder.evening-remaining-threshold:10}")
    private int eveningRemainingThreshold;

    public List<AppNotification> listLatest() {
        AppUser user = currentUserService.getCurrentUser();
        return notificationRepository.findTop50ByUserOrderByCreatedAtDesc(user);
    }

    public long unreadCount() {
        AppUser user = currentUserService.getCurrentUser();
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    public AppNotification markAsRead(Long id) {
        AppUser user = currentUserService.getCurrentUser();
        AppNotification notification = notificationRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + id));
        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }

    public ReminderSettingsResponse getReminderSettings() {
        return toReminderSettingsResponse(currentUserService.getCurrentUser());
    }

    public ReminderSettingsResponse updateReminderSettings(ReminderSettingsRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Reminder settings are required");
        }

        AppUser user = currentUserService.getCurrentUser();

        if (request.reviewRemindersEnabled() != null) {
            user.setReviewRemindersEnabled(request.reviewRemindersEnabled());
        }
        user.setPreferredReminderTime(parseReminderTime(request.preferredReminderTime(), user.getPreferredReminderTime()));

        if (request.eveningReminderEnabled() != null) {
            user.setEveningReminderEnabled(request.eveningReminderEnabled());
        }
        user.setEveningReminderTime(parseReminderTime(request.eveningReminderTime(), user.getEveningReminderTime()));

        if (request.comebackReminderEnabled() != null) {
            user.setComebackReminderEnabled(request.comebackReminderEnabled());
        }
        if (request.comebackReminderIntervalDays() != null) {
            user.setComebackReminderIntervalDays(clamp(request.comebackReminderIntervalDays(), 2, 3));
        }

        return toReminderSettingsResponse(appUserRepository.save(user));
    }

    @Scheduled(cron = "${app.reminder.cron}")
    public void dispatchReviewReminders() {
        dispatchReviewReminders(LocalDateTime.now());
    }

    void dispatchReviewReminders(LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.atTime(LocalTime.MAX);

        appUserRepository.findAll().forEach(user -> {
            if (!Boolean.TRUE.equals(user.getReviewRemindersEnabled())) {
                return;
            }

            boolean dailyReminderMinute = isReminderMinute(now, user.getPreferredReminderTime());
            boolean eveningReminderMinute = Boolean.TRUE.equals(user.getEveningReminderEnabled())
                    && isReminderMinute(now, user.getEveningReminderTime());

            if (!dailyReminderMinute && !eveningReminderMinute) {
                return;
            }

            long daysInactive = daysInactive(user, now);
            int dueCardsToday = 0;

            if (dailyReminderMinute) {
                dueCardsToday = Math.toIntExact(progressRepository.countDueWords(user, dayEnd));
                if (daysInactive >= 2) {
                    int overdueCards = Math.toIntExact(progressRepository.countOverdueWords(user, dayStart));
                    if (overdueCards > 0) {
                        dispatchComebackReminderIfNeeded(user, overdueCards, now);
                    } else {
                        dispatchDailyReminderIfNeeded(user, dueCardsToday, now, dayStart, dayEnd);
                    }
                } else {
                    dispatchDailyReminderIfNeeded(user, dueCardsToday, now, dayStart, dayEnd);
                }
            }

            if (daysInactive < 2 && eveningReminderMinute) {
                if (dueCardsToday == 0) {
                    dueCardsToday = Math.toIntExact(progressRepository.countDueWords(user, dayEnd));
                }
                dispatchEveningReminderIfNeeded(user, dueCardsToday, now, dayStart, dayEnd);
            }
        });
    }

    private void dispatchDailyReminderIfNeeded(
            AppUser user,
            int dueCardsToday,
            LocalDateTime now,
            LocalDateTime dayStart,
            LocalDateTime dayEnd
    ) {
        if (dueCardsToday <= 0 || alreadySentToday(user, AppNotification.NotificationType.DAILY_DUE_REMINDER, dayStart, dayEnd)) {
            return;
        }

        createNotification(
                user,
                "Đến giờ ôn tập",
                "Bạn có " + dueCardsToday + " thẻ cần ôn hôm nay.",
                AppNotification.NotificationType.DAILY_DUE_REMINDER,
                now
        );
    }

    private void dispatchEveningReminderIfNeeded(
            AppUser user,
            int remainingDueCards,
            LocalDateTime now,
            LocalDateTime dayStart,
            LocalDateTime dayEnd
    ) {
        boolean studiedToday = reviewEventRepository.existsByUserAndReviewedAtBetween(user, dayStart, dayEnd);
        boolean hasEnoughRemainingCards = remainingDueCards >= Math.max(1, eveningRemainingThreshold);

        if (remainingDueCards <= 0
                || alreadySentToday(user, AppNotification.NotificationType.EVENING_REVIEW_REMINDER, dayStart, dayEnd)
                || (studiedToday && !hasEnoughRemainingCards)) {
            return;
        }

        createNotification(
                user,
                "Nhắc nhẹ buổi tối",
                "Bạn còn " + remainingDueCards + " thẻ chưa ôn hôm nay.",
                AppNotification.NotificationType.EVENING_REVIEW_REMINDER,
                now
        );
    }

    private void dispatchComebackReminderIfNeeded(AppUser user, int overdueCards, LocalDateTime now) {
        if (!Boolean.TRUE.equals(user.getComebackReminderEnabled())) {
            return;
        }

        int intervalDays = clamp(user.getComebackReminderIntervalDays(), 2, 3);
        if (notificationRepository.existsByUserAndTypeAndScheduledAtAfter(
                user,
                AppNotification.NotificationType.OVERDUE_REMINDER,
                now.minusDays(intervalDays))) {
            return;
        }

        createNotification(
                user,
                "Quay lại ôn nhanh",
                "Bạn có " + overdueCards + " thẻ quá hạn. Ôn 10 phút để giữ nhịp nhé.",
                AppNotification.NotificationType.OVERDUE_REMINDER,
                now
        );
    }

    private void createNotification(
            AppUser user,
            String title,
            String message,
            AppNotification.NotificationType type,
            LocalDateTime scheduledAt
    ) {
        notificationRepository.save(AppNotification.builder()
                .user(user)
                .title(title)
                .message(message)
                .deepLink("/flashcards")
                .scheduledAt(scheduledAt)
                .type(type)
                .build());

        sendReminderEmail(user, title, message);
    }

    private boolean alreadySentToday(
            AppUser user,
            AppNotification.NotificationType type,
            LocalDateTime dayStart,
            LocalDateTime dayEnd
    ) {
        return notificationRepository.existsByUserAndTypeAndScheduledAtBetween(user, type, dayStart, dayEnd);
    }

    private boolean isReminderMinute(LocalDateTime now, LocalTime targetTime) {
        LocalTime nowMinute = now.toLocalTime().truncatedTo(ChronoUnit.MINUTES);
        LocalTime targetMinute = (targetTime == null ? DEFAULT_DAILY_REMINDER_TIME : targetTime).truncatedTo(ChronoUnit.MINUTES);
        return nowMinute.equals(targetMinute);
    }

    private long daysInactive(AppUser user, LocalDateTime now) {
        LocalDate lastActivityDate = reviewEventRepository.findTopByUserOrderByReviewedAtDesc(user)
                .map(ReviewEvent::getReviewedAt)
                .map(LocalDateTime::toLocalDate)
                .orElseGet(() -> {
                    LocalDateTime createdAt = user.getCreatedAt();
                    return createdAt == null ? now.toLocalDate() : createdAt.toLocalDate();
                });

        return ChronoUnit.DAYS.between(lastActivityDate, now.toLocalDate());
    }

    private ReminderSettingsResponse toReminderSettingsResponse(AppUser user) {
        return new ReminderSettingsResponse(
                Boolean.TRUE.equals(user.getReviewRemindersEnabled()),
                formatReminderTime(user.getPreferredReminderTime(), DEFAULT_DAILY_REMINDER_TIME),
                Boolean.TRUE.equals(user.getEveningReminderEnabled()),
                formatReminderTime(user.getEveningReminderTime(), DEFAULT_EVENING_REMINDER_TIME),
                Boolean.TRUE.equals(user.getComebackReminderEnabled()),
                clamp(user.getComebackReminderIntervalDays(), 2, 3),
                Math.max(1, eveningRemainingThreshold)
        );
    }

    private String formatReminderTime(LocalTime time, LocalTime fallback) {
        return (time == null ? fallback : time)
                .truncatedTo(ChronoUnit.MINUTES)
                .toString();
    }

    private LocalTime parseReminderTime(String value, LocalTime fallback) {
        if (value == null || value.isBlank()) {
            return fallback == null ? DEFAULT_DAILY_REMINDER_TIME : fallback;
        }

        try {
            return LocalTime.parse(value).truncatedTo(ChronoUnit.MINUTES);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Invalid reminder time: " + value);
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void sendReminderEmail(AppUser user, String subject, String reminderMessage) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail() == null || user.getEmail().isBlank() ? defaultReminderEmail : user.getEmail());
            message.setSubject(subject);
            message.setText(reminderMessage + "\n\nOpen: http://localhost:8080/flashcards");
            mailSender.send(message);
        } catch (Exception ignored) {
            // Mail is optional in local/dev environments.
        }
    }
}
