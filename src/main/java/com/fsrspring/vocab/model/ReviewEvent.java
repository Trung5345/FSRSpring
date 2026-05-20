package com.fsrspring.vocab.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_events", indexes = {
        @Index(name = "idx_review_events_user_word_reviewed_at", columnList = "user_id,word_id,reviewed_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewEvent {

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
    private Integer rating;

    @Column(nullable = false)
    private Boolean correct;

    @Column(nullable = false)
    @Builder.Default
    private Long responseTimeMs = 0L;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime reviewedAt = LocalDateTime.now();
}
