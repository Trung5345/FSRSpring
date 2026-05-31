package com.fsrspring.vocab.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(nullable = false)
    private Integer totalQuestions;

    @Column(nullable = false)
    @Builder.Default
    private Integer correctAnswers = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer incorrectAnswers = 0;

    @JsonProperty("createdAt")
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column
    private LocalDateTime completedAt;

    @Column(length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column
    private Word.DifficultyLevel difficulty;

    public double getScore() {
        if (totalQuestions == 0) return 0.0;
        return (double) correctAnswers / totalQuestions * 100;
    }
}
