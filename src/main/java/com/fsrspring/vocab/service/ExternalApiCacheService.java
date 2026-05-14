package com.fsrspring.vocab.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsrspring.vocab.model.ExternalApiCache;
import com.fsrspring.vocab.repository.ExternalApiCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExternalApiCacheService {

    private final ExternalApiCacheRepository cacheRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<JsonNode> getOrFetch(ExternalApiCache.Provider provider,
                                         String cacheKey,
                                         String paramsJson,
                                         Duration ttl,
                                         Supplier<JsonNode> fetcher) {
        String normalizedKey = normalize(cacheKey);
        String cacheHash = sha256(normalizedKey);
        LocalDateTime now = LocalDateTime.now();

        Optional<ExternalApiCache> cached = cacheRepository.findByProviderAndCacheHash(provider, cacheHash);
        if (cached.isPresent() && isFreshSuccess(cached.get(), now)) {
            return parseJson(cached.get().getResponseJson());
        }

        ExternalApiCache cache = cached.orElseGet(() -> ExternalApiCache.builder()
                .provider(provider)
                .cacheHash(cacheHash)
                .cacheKey(normalizedKey)
                .paramsJson(paramsJson)
                .build());

        try {
            JsonNode response = fetcher.get();
            cache.setFetchedAt(now);
            cache.setExpiresAt(now.plus(ttl));
            cache.setParamsJson(paramsJson);
            cache.setErrorMessage(null);
            if (response == null || response.isMissingNode() || response.isNull()) {
                cache.setStatus(ExternalApiCache.Status.NOT_FOUND);
                cache.setResponseJson(null);
                cacheRepository.save(cache);
                return Optional.empty();
            }
            cache.setStatus(ExternalApiCache.Status.SUCCESS);
            cache.setResponseJson(objectMapper.writeValueAsString(response));
            cacheRepository.save(cache);
            return Optional.of(response);
        } catch (Exception e) {
            cache.setFetchedAt(now);
            cache.setExpiresAt(now.plus(ttl));
            cache.setParamsJson(paramsJson);
            cache.setStatus(ExternalApiCache.Status.ERROR);
            cache.setResponseJson(null);
            cache.setErrorMessage(limit(e.getMessage(), 1000));
            cacheRepository.save(cache);
            log.warn("{} API fetch failed for '{}': {}", provider, normalizedKey, e.getMessage());
            return Optional.empty();
        }
    }

    private boolean isFreshSuccess(ExternalApiCache cache, LocalDateTime now) {
        return cache.getStatus() == ExternalApiCache.Status.SUCCESS
                && cache.getResponseJson() != null
                && (cache.getExpiresAt() == null || cache.getExpiresAt().isAfter(now));
    }

    private Optional<JsonNode> parseJson(String json) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readTree(json));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
