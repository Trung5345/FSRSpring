package com.fsrspring.vocab.service;

import com.fsrspring.vocab.dto.DatamuseWordDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Calls the Datamuse API (no API key required).
 * Docs: https://www.datamuse.com/api/
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DatamuseApiService {

    private static final String BASE_URL = "https://api.datamuse.com/words";
    private static final int MAX_RESULTS = 10;
    private static final int MAX_TOPIC_RESULTS = 50;

    private final RestTemplate restTemplate;

    @Cacheable(value = "datamuse-synonyms", key = "#word.toLowerCase()")
    public List<String> getSynonyms(String word) {
        return fetchWords("rel_syn=" + encode(word));
    }

    @Cacheable(value = "datamuse-antonyms", key = "#word.toLowerCase()")
    public List<String> getAntonyms(String word) {
        return fetchWords("rel_ant=" + encode(word));
    }

    @Cacheable(value = "datamuse-topic", key = "#topic.toLowerCase()")
    public List<DatamuseWordDto> getWordsByTopic(String topic) {
        return fetchWordDtos("topics=" + encode(topic) + "&max=" + MAX_RESULTS);
    }

    @Cacheable(value = "datamuse-topic", key = "#topic.toLowerCase() + ':' + #limit")
    public List<DatamuseWordDto> getWordsByTopic(String topic, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_TOPIC_RESULTS));
        return fetchWordDtos("topics=" + encode(topic) + "&max=" + safeLimit);
    }

    @Cacheable(value = "datamuse-meaning", key = "#word.toLowerCase() + ':' + #limit")
    public List<DatamuseWordDto> getWordsMeaningLike(String word, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_TOPIC_RESULTS));
        return fetchWordDtos("ml=" + encode(word) + "&max=" + safeLimit);
    }

    private List<String> fetchWords(String query) {
        String url = BASE_URL + "?" + query + "&max=" + MAX_RESULTS;
        try {
            DatamuseWordDto[] results = restTemplate.getForObject(url, DatamuseWordDto[].class);
            if (results == null) return Collections.emptyList();
            return Arrays.stream(results).map(DatamuseWordDto::getWord).toList();
        } catch (Exception e) {
            log.warn("Datamuse API error for query '{}': {}", query, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<DatamuseWordDto> fetchWordDtos(String query) {
        String url = BASE_URL + "?" + query;
        try {
            DatamuseWordDto[] results = restTemplate.getForObject(url, DatamuseWordDto[].class);
            if (results == null) return Collections.emptyList();
            return Arrays.asList(results);
        } catch (Exception e) {
            log.warn("Datamuse API error for query '{}': {}", query, e.getMessage());
            return Collections.emptyList();
        }
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
