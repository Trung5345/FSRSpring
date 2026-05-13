package com.linguist.repository;

import com.linguist.entity.ReviewLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReviewLogRepository extends JpaRepository<ReviewLog, Long> {

    List<ReviewLog> findByUserId(Long userId);

    List<ReviewLog> findByUserIdAndCardId(Long userId, Long cardId);

    List<ReviewLog> findByUserIdAndReviewedAtBetween(
            Long userId,
            LocalDateTime start,
            LocalDateTime end
    );

    long countByUserIdAndIsCorrectTrue(Long userId);

    long countByUserIdAndIsCorrectFalse(Long userId);
}