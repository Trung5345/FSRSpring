package com.fsrspring.vocab.repository;

import com.fsrspring.vocab.model.AppNotification;
import com.fsrspring.vocab.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {

    List<AppNotification> findTop50ByOrderByCreatedAtDesc();

    long countByIsReadFalse();

    List<AppNotification> findTop50ByUserOrderByCreatedAtDesc(AppUser user);

    long countByUserAndIsReadFalse(AppUser user);

    Optional<AppNotification> findByIdAndUser(Long id, AppUser user);

    boolean existsByUserAndTypeAndScheduledAtBetween(
            AppUser user,
            AppNotification.NotificationType type,
            LocalDateTime start,
            LocalDateTime end
    );

    boolean existsByUserAndTypeAndScheduledAtAfter(
            AppUser user,
            AppNotification.NotificationType type,
            LocalDateTime scheduledAt
    );
}
