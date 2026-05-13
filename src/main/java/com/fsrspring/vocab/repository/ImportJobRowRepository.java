package com.fsrspring.vocab.repository;

import com.fsrspring.vocab.model.ImportJobRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportJobRowRepository extends JpaRepository<ImportJobRow, Long> {
}
