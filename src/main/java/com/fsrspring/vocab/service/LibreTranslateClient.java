package com.fsrspring.vocab.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fsrspring.vocab.config.EnrichmentProperties;
import com.fsrspring.vocab.model.ExternalApiCache;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LibreTranslateClient {

    private static final Duration CACHE_TTL = Duration.ofDays(30);

    private final RestTemplate restTemplate;
    private final EnrichmentProperties properties;
    private final ExternalApiCacheService cacheService;

    public Optional<String> translateEnToVi(String text) {
        String cleaned = clean(text);
        String baseUrl = clean(properties.getApi().getLibretranslate().getBaseUrl());
        if (cleaned == null || baseUrl == null) {
            return Optional.empty();
        }
        String cacheKey = "en:vi:" + cleaned;
        return cacheService.getOrFetch(
                        ExternalApiCache.Provider.LIBRE_TRANSLATE,
                        cacheKey,
                        "{\"source\":\"en\",\"target\":\"vi\"}",
                        CACHE_TTL,
                        () -> postTranslate(baseUrl, cleaned))
                .map(node -> text(node.path("translatedText").asText(null)));
    }

    private JsonNode postTranslate(String baseUrl, String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("q", text);
        body.add("source", "en");
        body.add("target", "vi");
        body.add("format", "text");
        return restTemplate.postForObject(trimTrailingSlash(baseUrl) + "/translate", new HttpEntity<>(body, headers), JsonNode.class);
    }

    private String text(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned;
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
