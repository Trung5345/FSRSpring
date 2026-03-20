package com.fsrspring.vocab.repository;

import com.fsrspring.vocab.model.ReviewEvent;
import com.fsrspring.vocab.model.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewEventRepository extends JpaRepository<ReviewEvent, Long> {

    List<ReviewEvent> findTop20ByWordOrderByReviewedAtDesc(Word word);
}
