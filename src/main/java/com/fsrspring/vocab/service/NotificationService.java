package com.fsrspring.vocab.service;

import com.fsrspring.vocab.model.AppNotification;
import com.fsrspring.vocab.model.UserProgress;
import com.fsrspring.vocab.repository.AppNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final AppNotificationRepository notificationRepository;
    private final FsrsService fsrsService;
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
        List<UserProgress> dueWords = fsrsService.getDueWords(20);
        if (dueWords.isEmpty()) {
            return;
        }

        String message = "You have " + dueWords.size() + " vocabulary cards due for review.";

        notificationRepository.save(AppNotification.builder()
                .title("Time to review")
                .message(message)
                .deepLink("/learn?mode=fsrs")
                .scheduledAt(LocalDateTime.now())
                .type(AppNotification.NotificationType.REVIEW_REMINDER)
                .build());

        sendReminderEmail(message);
    }

    private void sendReminderEmail(String reminderMessage) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(defaultReminderEmail);
            message.setSubject("FSRS review reminder");
            message.setText(reminderMessage + "\n\nOpen: http://localhost:8080/learn?mode=fsrs");
            mailSender.send(message);
        } catch (Exception ignored) {
            // Mail is optional in local/dev environments.
        }
    }
}
