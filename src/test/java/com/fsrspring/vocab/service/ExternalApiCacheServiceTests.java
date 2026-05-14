package com.fsrspring.vocab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsrspring.vocab.model.ExternalApiCache;
import com.fsrspring.vocab.repository.ExternalApiCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ExternalApiCacheServiceTests {

    @Autowired
    private ExternalApiCacheService cacheService;

    @Autowired
    private ExternalApiCacheRepository cacheRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void reset() {
        cacheRepository.deleteAll();
    }

    @Test
    void getOrFetchReusesFreshSuccessfulResponse() throws Exception {
        AtomicInteger calls = new AtomicInteger();

        String first = cacheService.getOrFetch(
                        ExternalApiCache.Provider.LIBRE_TRANSLATE,
                        "en:vi:meticulous",
                        "{}",
                        Duration.ofDays(1),
                        () -> {
                            calls.incrementAndGet();
                            return objectMapper.createObjectNode().put("translatedText", "ti mi");
                        })
                .orElseThrow()
                .path("translatedText")
                .asText();

        String second = cacheService.getOrFetch(
                        ExternalApiCache.Provider.LIBRE_TRANSLATE,
                        "en:vi:meticulous",
                        "{}",
                        Duration.ofDays(1),
                        () -> {
                            calls.incrementAndGet();
                            return objectMapper.createObjectNode().put("translatedText", "changed");
                        })
                .orElseThrow()
                .path("translatedText")
                .asText();

        assertThat(first).isEqualTo("ti mi");
        assertThat(second).isEqualTo("ti mi");
        assertThat(calls).hasValue(1);
        assertThat(cacheRepository.findAll()).hasSize(1);
    }

    @Test
    void getOrFetchStoresErrorsWithoutThrowing() {
        assertThat(cacheService.getOrFetch(
                ExternalApiCache.Provider.WIKTAPI,
                "definitions:en:missing",
                "{}",
                Duration.ofDays(1),
                () -> {
                    throw new IllegalStateException("provider down");
                })).isEmpty();

        ExternalApiCache cache = cacheRepository.findAll().get(0);
        assertThat(cache.getStatus()).isEqualTo(ExternalApiCache.Status.ERROR);
        assertThat(cache.getErrorMessage()).contains("provider down");
    }
}
