package com.fsrspring.vocab.repository;

import com.fsrspring.vocab.model.CefrLevel;
import com.fsrspring.vocab.model.VocabularySet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VocabularySetRepository extends JpaRepository<VocabularySet, Long> {
    List<VocabularySet> findByTopicId(Long topicId);
    List<VocabularySet> findByCefrLevel(CefrLevel cefrLevel);
}
