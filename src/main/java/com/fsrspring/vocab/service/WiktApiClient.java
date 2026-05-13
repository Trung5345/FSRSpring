package com.fsrspring.vocab.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fsrspring.vocab.config.EnrichmentProperties;
import com.fsrspring.vocab.dto.ProviderWordData;
import com.fsrspring.vocab.model.ExternalApiCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WiktApiClient {

    private static final Duration CACHE_TTL = Duration.ofDays(30);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final EnrichmentProperties properties;
    private final ExternalApiCacheService cacheService;

    public ProviderWordData lookupEnglish(String word) {
        String normalized = normalizeWord(word);
        String baseUrl = clean(properties.getApi().getWiktapi().getBaseUrl());
        if (normalized == null || baseUrl == null) {
            return ProviderWordData.builder().build();
        }

        Optional<JsonNode> definitions = fetchEndpoint(baseUrl, normalized, "definitions");
        Optional<JsonNode> pronunciations = fetchEndpoint(baseUrl, normalized, "pronunciations");
        Optional<JsonNode> translations = fetchEndpoint(baseUrl, normalized, "translations");

        ObjectNode raw = objectMapper.createObjectNode();
        definitions.ifPresent(node -> raw.set("definitions", node));
        pronunciations.ifPresent(node -> raw.set("pronunciations", node));
        translations.ifPresent(node -> raw.set("translations", node));

        return ProviderWordData.builder()
                .partOfSpeech(firstText(definitions.orElse(null), "definitions", "pos"))
                .definition(firstGloss(definitions.orElse(null)))
                .example(firstExample(definitions.orElse(null)))
                .pronunciation(firstSoundValue(pronunciations.orElse(null), "ipa"))
                .audioUrl(firstSoundValue(pronunciations.orElse(null), "audio"))
                .rawJson(raw.size() == 0 ? null : raw)
                .build();
    }

    private Optional<JsonNode> fetchEndpoint(String baseUrl, String word, String endpoint) {
        String cacheKey = endpoint + ":en:" + word;
        return cacheService.getOrFetch(
                ExternalApiCache.Provider.WIKTAPI,
                cacheKey,
                "{\"edition\":\"en\",\"lang\":\"en\",\"endpoint\":\"" + endpoint + "\"}",
                CACHE_TTL,
                () -> restTemplate.getForObject(buildUrl(baseUrl, word, endpoint), JsonNode.class)
        );
    }

    private String buildUrl(String baseUrl, String word, String endpoint) {
        return UriComponentsBuilder.fromHttpUrl(trimTrailingSlash(baseUrl))
                .pathSegment("v1", "en", "word", word, endpoint)
                .queryParam("lang", "en")
                .toUriString();
    }

    private String firstText(JsonNode node, String arrayName, String fieldName) {
        if (node == null) return null;
        JsonNode array = node.path(arrayName);
        if (!array.isArray()) return null;
        for (JsonNode item : array) {
            String value = text(item.path(fieldName).asText(null));
            if (value != null) return value;
        }
        return null;
    }

    private String firstGloss(JsonNode node) {
        if (node == null) return null;
        for (JsonNode definition : node.path("definitions")) {
            for (JsonNode sense : definition.path("senses")) {
                for (JsonNode gloss : sense.path("glosses")) {
                    String value = text(gloss.asText(null));
                    if (value != null) return value;
                }
            }
        }
        return null;
    }

    private String firstExample(JsonNode node) {
        if (node == null) return null;
        for (JsonNode definition : node.path("definitions")) {
            for (JsonNode sense : definition.path("senses")) {
                for (JsonNode example : sense.path("examples")) {
                    if (example.isTextual()) {
                        String value = text(example.asText());
                        if (value != null) return value;
                    }
                    String text = text(example.path("text").asText(null));
                    if (text != null) return text;
                    String english = text(example.path("english").asText(null));
                    if (english != null) return english;
                }
            }
        }
        return null;
    }

    private String firstSoundValue(JsonNode node, String fieldName) {
        if (node == null) return null;
        for (JsonNode pronunciation : node.path("pronunciations")) {
            for (JsonNode sound : pronunciation.path("sounds")) {
                String value = text(sound.path(fieldName).asText(null));
                if (value != null) return normalizeAudio(value);
            }
        }
        return null;
    }

    private String normalizeAudio(String value) {
        if (value == null) return null;
        return value.startsWith("//") ? "https:" + value : value;
    }

    private String normalizeWord(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.toLowerCase();
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
