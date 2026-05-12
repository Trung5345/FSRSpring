package com.fsrspring.vocab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @NotBlank(message = "Translation is required")
    @Column(nullable = false, length = 500)
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "topic_id")
    @JsonIgnoreProperties({"words"})
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

    public enum DifficultyLevel {
        BEGINNER, INTERMEDIATE, ADVANCED
    }
}
