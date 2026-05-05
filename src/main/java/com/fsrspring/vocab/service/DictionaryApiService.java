package com.fsrspring.vocab.service;

import com.fsrspring.vocab.dto.DictionaryApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

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

    private final RestTemplate restTemplate;

    /**
     * Look up a word from the Free Dictionary API.
     * Returns empty Optional if the word is not found or API is unavailable.
     */
    @Cacheable(value = "dictionary", key = "#word.toLowerCase()")
    public Optional<DictionaryApiResponse> lookup(String word) {
        String url = BASE_URL + word.trim().toLowerCase();
        try {
            DictionaryApiResponse[] responses = restTemplate.getForObject(url, DictionaryApiResponse[].class);
            if (responses != null && responses.length > 0) {
                return Optional.of(responses[0]);
            }
            return Optional.empty();
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("Word '{}' not found in dictionary API", word);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Dictionary API error for word '{}': {}", word, e.getMessage());
            return Optional.empty();
        }
    }
}
