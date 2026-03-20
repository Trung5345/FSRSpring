package com.fsrspring.vocab.repository;

import com.fsrspring.vocab.model.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {

    List<AppNotification> findTop50ByOrderByCreatedAtDesc();

    long countByIsReadFalse();
}
