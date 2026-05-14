package com.fsrspring.vocab.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsrspring.vocab.config.EnrichmentProperties;
import com.fsrspring.vocab.dto.ImageCandidate;
import com.fsrspring.vocab.model.ExternalApiCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PixabayImageClient {

    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final EnrichmentProperties properties;
    private final ExternalApiCacheService cacheService;

    public Optional<ImageCandidate> searchFirstPhoto(String query) {
        String cleaned = clean(query);
        String apiKey = clean(properties.getApi().getPixabay().getKey());
        String baseUrl = clean(properties.getApi().getPixabay().getBaseUrl());
        if (cleaned == null || apiKey == null || baseUrl == null) {
            return Optional.empty();
        }

        return cacheService.getOrFetch(
                        ExternalApiCache.Provider.PIXABAY,
                        "photo:" + cleaned,
                        "{\"image_type\":\"photo\",\"safesearch\":true,\"per_page\":3}",
                        CACHE_TTL,
                        () -> restTemplate.getForObject(buildUrl(baseUrl, apiKey, cleaned), JsonNode.class))
                .flatMap(this::toCandidate);
    }

    private String buildUrl(String baseUrl, String apiKey, String query) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("key", apiKey)
                .queryParam("q", query)
                .queryParam("image_type", "photo")
                .queryParam("safesearch", "true")
                .queryParam("per_page", 3)
                .toUriString();
    }

    private Optional<ImageCandidate> toCandidate(JsonNode node) {
        JsonNode hits = node.path("hits");
        if (!hits.isArray() || hits.isEmpty()) {
            return Optional.empty();
        }
        JsonNode hit = hits.get(0);
        String imageUrl = firstNonBlank(
                hit.path("largeImageURL").asText(null),
                hit.path("webformatURL").asText(null),
                hit.path("previewURL").asText(null)
        );
        if (imageUrl == null) {
            return Optional.empty();
        }
        String user = clean(hit.path("user").asText(null));
        String pageUrl = clean(hit.path("pageURL").asText(null));
        return Optional.of(ImageCandidate.builder()
                .provider("PIXABAY")
                .providerImageId(hit.path("id").asText(""))
                .downloadUrl(imageUrl)
                .pageUrl(pageUrl)
                .attribution(user != null ? "Image by " + user + " on Pixabay" : "Image from Pixabay")
                .metadataJson(toJson(hit))
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
}
