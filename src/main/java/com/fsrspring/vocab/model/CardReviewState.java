package com.fsrspring.vocab.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "card_review_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardReviewState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne
    @JoinColumn(name = "card_id")
    private Card card;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "ease_factor")
    private Double easeFactor;

    @Column(name = "interval_days")
    private Integer intervalDays;

    @Column(name = "repetition_count")
    private Integer repetitionCount;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;
}