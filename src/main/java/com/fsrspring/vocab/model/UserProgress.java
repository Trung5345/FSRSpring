package com.fsrspring.vocab.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_progress", indexes = {
    @Index(name = "idx_user_progress_next_review", columnList = "nextReview"),
    @Index(name = "idx_user_progress_mastery", columnList = "mastery")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", nullable = false)
    private Word word;


    @Column(nullable = false)
    @Builder.Default
    private Integer correctCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer incorrectCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MasteryLevel mastery = MasteryLevel.NEW;

    @Column
    private LocalDateTime lastStudied;

    @Column
    private LocalDateTime nextReview;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // FSRS core state
    @Column(nullable = false)
    @Builder.Default
    private Double fsrsStability = 0.2;

    @Column(nullable = false)
    @Builder.Default
    private Double fsrsDifficulty = 5.0;

    @Column(nullable = false)
    @Builder.Default
    private Integer fsrsRepetition = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer fsrsLapseCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Double fsrsRetrievability = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double lastIntervalHours = 0.0;

    // RNN-like sequential behavior representation
    @Column(nullable = false)
    @Builder.Default
    private Double sequenceAccuracyEMA = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double sequenceResponseMsEMA = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double sequenceConsistency = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double sequenceDifficultyTrend = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Integer sequenceStep = 0;

    public enum MasteryLevel {
        NEW, LEARNING, REVIEWING, MASTERED
    }

    public int getTotalAttempts() {
        return correctCount + incorrectCount;
    }

    public double getAccuracy() {
        int total = getTotalAttempts();
        if (total == 0) return 0.0;
        return (double) correctCount / total * 100;
    }
}
