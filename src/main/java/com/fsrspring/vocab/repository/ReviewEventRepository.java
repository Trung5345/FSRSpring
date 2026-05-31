package com.fsrspring.vocab.repository;

import com.fsrspring.vocab.model.ReviewEvent;
import com.fsrspring.vocab.model.AppUser;
import com.fsrspring.vocab.model.Word;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewEventRepository extends JpaRepository<ReviewEvent, Long> {

    List<ReviewEvent> findTop20ByWordOrderByReviewedAtDesc(Word word);

    List<ReviewEvent> findByWord(Word word);

    Optional<ReviewEvent> findTopByUserOrderByReviewedAtDesc(AppUser user);

    boolean existsByUserAndReviewedAtBetween(AppUser user, LocalDateTime start, LocalDateTime end);

    @EntityGraph(attributePaths = {"word"})
    List<ReviewEvent> findByUserOrderByReviewedAtDesc(AppUser user, Pageable pageable);
}
