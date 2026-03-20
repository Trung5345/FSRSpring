package com.fsrspring.vocab.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "trusted_flashcards", indexes = {
        @Index(name = "idx_flashcard_word", columnList = "word"),
        @Index(name = "idx_flashcard_topic_level", columnList = "topic,level")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrustedFlashcard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String word;

    @Column(nullable = false, length = 500)
    private String translation;

    @Column(length = 1000)
    private String example;

    @Column(length = 100)
    private String topic;

    @Column(length = 100)
    private String level;

    @Column(nullable = false, length = 200)
    private String sourceName;

    @Column(length = 1000)
    private String sourceUrl;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime importedAt = LocalDateTime.now();
}
