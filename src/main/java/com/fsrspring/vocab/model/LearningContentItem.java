package com.fsrspring.vocab.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "learning_content_items", indexes = {
        @Index(name = "idx_learning_content_source_topic", columnList = "sourceType,topic"),
        @Index(name = "idx_learning_content_published", columnList = "publishedAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningContentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceType sourceType;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(length = 2000)
    private String summary;

    @Column(length = 300)
    private String thumbnailUrl;

    @Column(length = 100)
    private String topic;

    @Column
    private LocalDateTime publishedAt;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime fetchedAt = LocalDateTime.now();

    public enum SourceType {
        YOUTUBE,
        NEWS
    }
}
