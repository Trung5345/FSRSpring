package com.fsrspring.vocab.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsrspring.vocab.dto.DictionaryApiResponse;
import com.fsrspring.vocab.model.ExternalApiCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Optional;

/**
 * Calls the Free Dictionary API (no API key required).
 * API: https://api.dictionaryapi.dev/api/v2/entries/en/{word}
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DictionaryApiService {

    private static final String BASE_URL = "https://api.dictionaryapi.dev/api/v2/entries/en/";
    private static final Duration CACHE_TTL = Duration.ofDays(30);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ExternalApiCacheService cacheService;

    /**
     * Look up a word from the Free Dictionary API.
     * Returns empty Optional if the word is not found or API is unavailable.
     */
    @Cacheable(value = "dictionary", key = "#word.toLowerCase()")
    public Optional<DictionaryApiResponse> lookup(String word) {
        String cleaned = word.trim().toLowerCase();
        return cacheService.getOrFetch(
                        ExternalApiCache.Provider.FREE_DICTIONARY,
                        cleaned,
                        "{}",
                        CACHE_TTL,
                        () -> fetch(cleaned))
                .flatMap(this::toResponse);
    }

    private JsonNode fetch(String word) {
        String url = BASE_URL + word;
        try {
            return restTemplate.getForObject(url, JsonNode.class);
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("Word '{}' not found in dictionary API", word);
            return null;
        }
    }

    private Optional<DictionaryApiResponse> toResponse(JsonNode node) {
        if (node == null || !node.isArray() || node.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.treeToValue(node.get(0), DictionaryApiResponse.class));
        } catch (Exception e) {
            log.warn("Could not map dictionary response: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
