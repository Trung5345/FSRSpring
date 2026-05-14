package com.fsrspring.vocab.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "external_api_cache",
        uniqueConstraints = @UniqueConstraint(name = "uk_external_api_cache_provider_hash", columnNames = {"provider", "cache_hash"}),
        indexes = {
                @Index(name = "idx_external_api_cache_provider_hash", columnList = "provider, cache_hash"),
                @Index(name = "idx_external_api_cache_expires_at", columnList = "expires_at")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalApiCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Provider provider;

    @Column(name = "cache_hash", nullable = false, length = 64)
    private String cacheHash;

    @Column(nullable = false, length = 1000)
    private String cacheKey;

    @Column(columnDefinition = "LONGTEXT")
    private String paramsJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(columnDefinition = "LONGTEXT")
    private String responseJson;

    @Column(length = 1000)
    private String errorMessage;

    @Column(name = "fetched_at", nullable = false)
    @Builder.Default
    private LocalDateTime fetchedAt = LocalDateTime.now();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public enum Provider {
        LIBRE_TRANSLATE,
        WIKTAPI,
        FREE_DICTIONARY,
        DATAMUSE,
        PIXABAY,
        PEXELS
    }

    public enum Status {
        SUCCESS,
        NOT_FOUND,
        ERROR
    }
}
