package com.fsrspring.vocab.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsrspring.vocab.config.EnrichmentProperties;
import com.fsrspring.vocab.dto.ImageCandidate;
import com.fsrspring.vocab.model.ExternalApiCache;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PexelsImageClient {

    private static final Duration CACHE_TTL = Duration.ofDays(7);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final EnrichmentProperties properties;
    private final ExternalApiCacheService cacheService;

    public Optional<ImageCandidate> searchFirstPhoto(String query) {
        String cleaned = clean(query);
        String apiKey = clean(properties.getApi().getPexels().getKey());
        String baseUrl = clean(properties.getApi().getPexels().getBaseUrl());
        if (cleaned == null || apiKey == null || baseUrl == null) {
            return Optional.empty();
        }

        return cacheService.getOrFetch(
                        ExternalApiCache.Provider.PEXELS,
                        "photo:" + cleaned,
                        "{\"per_page\":3,\"orientation\":\"landscape\"}",
                        CACHE_TTL,
                        () -> search(baseUrl, apiKey, cleaned))
                .flatMap(this::toCandidate);
    }

    private JsonNode search(String baseUrl, String apiKey, String query) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiKey);
        String url = UriComponentsBuilder.fromHttpUrl(trimTrailingSlash(baseUrl))
                .pathSegment("search")
                .queryParam("query", query)
                .queryParam("per_page", 3)
                .queryParam("orientation", "landscape")
                .toUriString();
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class).getBody();
    }

    private Optional<ImageCandidate> toCandidate(JsonNode node) {
        JsonNode photos = node.path("photos");
        if (!photos.isArray() || photos.isEmpty()) {
            return Optional.empty();
        }
        JsonNode photo = photos.get(0);
        JsonNode src = photo.path("src");
        String imageUrl = firstNonBlank(
                src.path("large").asText(null),
                src.path("medium").asText(null),
                src.path("original").asText(null)
        );
        if (imageUrl == null) {
            return Optional.empty();
        }
        String photographer = clean(photo.path("photographer").asText(null));
        String pageUrl = clean(photo.path("url").asText(null));
        return Optional.of(ImageCandidate.builder()
                .provider("PEXELS")
                .providerImageId(photo.path("id").asText(""))
                .downloadUrl(imageUrl)
                .pageUrl(pageUrl)
                .attribution(photographer != null ? "Photo by " + photographer + " on Pexels" : "Photo from Pexels")
                .metadataJson(toJson(photo))
                .build());
    }

    private String toJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String cleaned = clean(value);
            if (cleaned != null) return cleaned;
        }
        return null;
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
