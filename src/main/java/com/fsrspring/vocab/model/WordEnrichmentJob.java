package com.fsrspring.vocab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "word_enrichment_jobs", indexes = {
        @Index(name = "idx_word_enrichment_job_word", columnList = "word_id"),
        @Index(name = "idx_word_enrichment_job_status_next_run", columnList = "status, next_run_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordEnrichmentJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "word_id", nullable = false)
    @JsonIgnoreProperties({"topic"})
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Word word;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(name = "next_run_at", nullable = false)
    @Builder.Default
    private LocalDateTime nextRunAt = LocalDateTime.now();

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Column(length = 1000)
    private String lastError;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum Status {
        PENDING,
        RUNNING,
        PARTIAL,
        COMPLETED,
        FAILED
    }
}
