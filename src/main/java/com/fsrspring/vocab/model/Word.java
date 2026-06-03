package com.fsrspring.vocab.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "words")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Word is required")
    @Column(nullable = false, length = 200)
    private String word;

    @Column(length = 500)
    private String translation;

    @Column(length = 1000)
    private String example;

    @Column(length = 200)
    private String pronunciation;

    @Column(length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DifficultyLevel difficulty = DifficultyLevel.BEGINNER;

    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Enriched fields (Free Dictionary API + Datamuse) ──

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    @JsonIgnoreProperties({"words"})
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Topic topic;

    @Enumerated(EnumType.STRING)
    private CefrLevel cefrLevel;

    @Column(length = 50)
    private String partOfSpeech;

    @Column(length = 500)
    private String audioUrl;

    @Column(length = 1000)
    private String synonyms;

    @Column(length = 1000)
    private String antonyms;

    @Column(length = 2000)
    private String origin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EnrichmentStatus enrichmentStatus = EnrichmentStatus.NOT_REQUESTED;

    @Column(columnDefinition = "LONGTEXT")
    @JsonIgnore
    private String enrichmentJson;

    @Column(columnDefinition = "LONGTEXT")
    @JsonIgnore
    private String imageMetadataJson;

    private LocalDateTime enrichedAt;

    public enum DifficultyLevel {
        BEGINNER, INTERMEDIATE, ADVANCED
    }

    public enum EnrichmentStatus {
        NOT_REQUESTED,
        PENDING,
        RUNNING,
        PARTIAL,
        COMPLETED,
        FAILED
    }
}
