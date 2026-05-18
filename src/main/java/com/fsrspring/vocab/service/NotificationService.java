package com.fsrspring.vocab.service;

import com.fsrspring.vocab.model.AppNotification;
import com.fsrspring.vocab.model.AppUser;
import com.fsrspring.vocab.model.UserProgress;
import com.fsrspring.vocab.repository.AppNotificationRepository;
import com.fsrspring.vocab.repository.UserProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final AppNotificationRepository notificationRepository;
    private final UserProgressRepository progressRepository;
    private final JavaMailSender mailSender;

    @Value("${app.reminder.default-email}")
    private String defaultReminderEmail;

    public List<AppNotification> listLatest() {
        return notificationRepository.findTop50ByOrderByCreatedAtDesc();
    }

    public long unreadCount() {
        return notificationRepository.countByIsReadFalse();
    }

    public AppNotification markAsRead(Long id) {
        AppNotification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + id));
        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }

    @Scheduled(cron = "${app.reminder.cron}")
    public void dispatchReviewReminders() {
        Map<AppUser, List<UserProgress>> dueWordsByUser = progressRepository.findDueWordsForAllUsers(LocalDateTime.now())
                .stream()
                .collect(Collectors.groupingBy(UserProgress::getUser));

        if (dueWordsByUser.isEmpty()) {
            return;
        }

        LocalDateTime scheduledAt = LocalDateTime.now();
        dueWordsByUser.forEach((user, dueWords) -> {
            String message = "You have " + dueWords.size() + " vocabulary cards due for review.";

            notificationRepository.save(AppNotification.builder()
                    .user(user)
                    .title("Time to review")
                    .message(message)
                    .deepLink("/learn?mode=fsrs")
                    .scheduledAt(scheduledAt)
                    .type(AppNotification.NotificationType.REVIEW_REMINDER)
                    .build());

            sendReminderEmail(user, message);
        });
    }

    private void sendReminderEmail(AppUser user, String reminderMessage) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail() == null || user.getEmail().isBlank() ? defaultReminderEmail : user.getEmail());
            message.setSubject("FSRS review reminder");
            message.setText(reminderMessage + "\n\nOpen: http://localhost:8080/learn?mode=fsrs");
            mailSender.send(message);
        } catch (Exception ignored) {
            // Mail is optional in local/dev environments.
        }
    }
}
