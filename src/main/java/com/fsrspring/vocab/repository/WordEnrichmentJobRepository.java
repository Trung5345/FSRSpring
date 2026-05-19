package com.fsrspring.vocab.repository;

import com.fsrspring.vocab.model.WordEnrichmentJob;
import com.fsrspring.vocab.model.Word;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WordEnrichmentJobRepository extends JpaRepository<WordEnrichmentJob, Long> {
    boolean existsByWordIdAndStatusIn(Long wordId, Collection<WordEnrichmentJob.Status> statuses);

    Optional<WordEnrichmentJob> findTopByWordIdOrderByCreatedAtDesc(Long wordId);

    List<WordEnrichmentJob> findByWord(Word word);

    @EntityGraph(attributePaths = "word")
    List<WordEnrichmentJob> findTop10ByStatusAndNextRunAtLessThanEqualOrderByCreatedAtAsc(
            WordEnrichmentJob.Status status,
            LocalDateTime nextRunAt
    );

    @Modifying
    @Query("UPDATE WordEnrichmentJob j SET j.status = com.fsrspring.vocab.model.WordEnrichmentJob.Status.PENDING, j.nextRunAt = :now WHERE j.status = com.fsrspring.vocab.model.WordEnrichmentJob.Status.RUNNING")
    int resetRunningJobsToPending(LocalDateTime now);
}
