package com.fsrspring.vocab.repository;

import com.fsrspring.vocab.model.TrustedFlashcard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrustedFlashcardRepository extends JpaRepository<TrustedFlashcard, Long> {

    @Query("SELECT tf FROM TrustedFlashcard tf WHERE LOWER(tf.word) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(tf.translation) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY tf.importedAt DESC")
    List<TrustedFlashcard> searchByKeyword(String keyword);

    List<TrustedFlashcard> findTop100ByOrderByImportedAtDesc();

    List<TrustedFlashcard> findBySourceNameIgnoreCaseAndTopicIgnoreCaseAndWordIgnoreCaseOrderByImportedAtDesc(
            String sourceName,
            String topic,
            String word);

    boolean existsByWordIgnoreCase(String word);
}
