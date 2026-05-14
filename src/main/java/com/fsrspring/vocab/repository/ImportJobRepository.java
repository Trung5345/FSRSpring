package com.fsrspring.vocab.repository;

import com.fsrspring.vocab.model.ImportJob;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {
    List<ImportJob> findTop50ByOrderByCreatedAtDesc();

    @Override
    @EntityGraph(attributePaths = "rows")
    Optional<ImportJob> findById(Long id);
}
