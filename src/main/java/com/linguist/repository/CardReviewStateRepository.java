package com.linguist.repository;

import com.linguist.entity.CardReviewState;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CardReviewStateRepository extends JpaRepository<CardReviewState, Long> {

    Optional<CardReviewState> findByUserIdAndCardId(Long userId, Long cardId);

    List<CardReviewState> findByUserIdAndDueAtLessThanEqualOrderByDueAtAsc(
            Long userId,
            LocalDateTime now,
            Pageable pageable
    );

    long countByUserIdAndDueAtLessThanEqual(Long userId, LocalDateTime now);
}