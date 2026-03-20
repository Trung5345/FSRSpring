package com.fsrspring.vocab.service;

import com.fsrspring.vocab.model.LearningContentItem;
import com.fsrspring.vocab.repository.LearningContentItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ContentService {

    private final LearningContentItemRepository contentRepository;

    @Value("${app.api.youtube.key:}")
    private String youtubeApiKey;

    @Value("${app.api.youtube.base-url}")
    private String youtubeBaseUrl;

    @Value("${app.api.news.key:}")
    private String newsApiKey;

    @Value("${app.api.news.base-url}")
    private String newsBaseUrl;

    @Cacheable("youtube-content")
    public List<LearningContentItem> fetchYoutube(String topic) {
        if (youtubeApiKey == null || youtubeApiKey.isBlank()) {
            return List.of();
        }

        RestClient client = RestClient.builder().baseUrl(youtubeBaseUrl).build();
        Map<String, Object> response = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("part", "snippet")
                        .queryParam("q", topic + " english vocabulary")
                        .queryParam("type", "video")
                        .queryParam("maxResults", 10)
                        .queryParam("key", youtubeApiKey)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Map.class);

        List<LearningContentItem> items = new ArrayList<>();
        Object rawItems = response != null ? response.get("items") : null;
        if (rawItems instanceof List<?> list) {
            for (Object o : list) {
                if (!(o instanceof Map<?, ?> row)) {
                    continue;
                }
                Map<?, ?> id = mapValue(row.get("id"));
                Map<?, ?> snippet = mapValue(row.get("snippet"));
                String videoId = stringValue(id.get("videoId"));
                if (videoId.isBlank()) {
                    continue;
                }

                Map<?, ?> thumbnails = mapValue(snippet.get("thumbnails"));
                Map<?, ?> medium = mapValue(thumbnails.get("medium"));

                LearningContentItem item = LearningContentItem.builder()
                        .sourceType(LearningContentItem.SourceType.YOUTUBE)
                        .title(stringValue(snippet.get("title")))
                        .summary(stringValue(snippet.get("description")))
                        .url("https://www.youtube.com/watch?v=" + videoId)
                        .thumbnailUrl(stringValue(medium.get("url")))
                        .topic(topic)
                        .publishedAt(parseDateTime(stringValue(snippet.get("publishedAt"))))
                        .build();
                items.add(contentRepository.save(item));
            }
        }

        return items;
    }

    @Cacheable("news-content")
    public List<LearningContentItem> fetchNews(String topic) {
        if (newsApiKey == null || newsApiKey.isBlank()) {
            return List.of();
        }

        RestClient client = RestClient.builder().baseUrl(newsBaseUrl).build();
        Map<String, Object> response = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/everything")
                        .queryParam("q", topic + " english learning")
                        .queryParam("language", "en")
                        .queryParam("sortBy", "publishedAt")
                        .queryParam("pageSize", 10)
                        .queryParam("apiKey", newsApiKey)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Map.class);

        List<LearningContentItem> items = new ArrayList<>();
        Object rawArticles = response != null ? response.get("articles") : null;
        if (rawArticles instanceof List<?> list) {
            for (Object o : list) {
                if (!(o instanceof Map<?, ?> row)) {
                    continue;
                }
                LearningContentItem item = LearningContentItem.builder()
                        .sourceType(LearningContentItem.SourceType.NEWS)
                        .title(stringValue(row.get("title")))
                        .summary(stringValue(row.get("description")))
                        .url(stringValue(row.get("url")))
                        .thumbnailUrl(stringValue(row.get("urlToImage")))
                        .topic(topic)
                        .publishedAt(parseDateTime(stringValue(row.get("publishedAt"))))
                        .build();
                items.add(contentRepository.save(item));
            }
        }

        return items;
    }

    public Map<String, Object> fetchCombined(String topic) {
        List<LearningContentItem> youtube = fetchYoutube(topic);
        List<LearningContentItem> news = fetchNews(topic);

        Map<String, Object> payload = new HashMap<>();
        payload.put("topic", topic);
        payload.put("youtube", youtube);
        payload.put("news", news);
        payload.put("total", youtube.size() + news.size());
        return payload;
    }

    private Map<?, ?> mapValue(Object obj) {
        if (obj instanceof Map<?, ?> m) {
            return m;
        }
        return Map.of();
    }

    private String stringValue(Object obj) {
        return obj == null ? "" : String.valueOf(obj);
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
