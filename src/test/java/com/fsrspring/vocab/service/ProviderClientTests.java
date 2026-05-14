package com.fsrspring.vocab.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsrspring.vocab.config.EnrichmentProperties;
import com.fsrspring.vocab.dto.ImageCandidate;
import com.fsrspring.vocab.dto.ProviderWordData;
import com.fsrspring.vocab.model.ExternalApiCache;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProviderClientTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void libreTranslateClientPostsTextThroughCacheAndMapsTranslation() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ExternalApiCacheService cacheService = mock(ExternalApiCacheService.class);
        EnrichmentProperties properties = new EnrichmentProperties();
        properties.getApi().getLibretranslate().setBaseUrl("http://libretranslate:5000/");

        when(restTemplate.postForObject(eq("http://libretranslate:5000/translate"), any(), eq(JsonNode.class)))
                .thenReturn(objectMapper.createObjectNode().put("translatedText", "ti mi"));
        when(cacheService.getOrFetch(eq(ExternalApiCache.Provider.LIBRE_TRANSLATE), eq("en:vi:meticulous"),
                anyString(), any(Duration.class), any()))
                .thenAnswer(invocation -> Optional.of(invocation.<Supplier<JsonNode>>getArgument(4).get()));

        LibreTranslateClient client = new LibreTranslateClient(restTemplate, properties, cacheService);

        assertThat(client.translateEnToVi("meticulous")).contains("ti mi");
    }

    @Test
    void wiktApiClientMapsDefinitionExampleIpaAndAudio() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ExternalApiCacheService cacheService = mock(ExternalApiCacheService.class);
        EnrichmentProperties properties = new EnrichmentProperties();
        properties.getApi().getWiktapi().setBaseUrl("https://api.wiktapi.dev");

        when(cacheService.getOrFetch(eq(ExternalApiCache.Provider.WIKTAPI), startsWith("definitions:"),
                anyString(), any(Duration.class), any())).thenReturn(Optional.of(read("""
                {
                  "definitions": [{
                    "pos": "adjective",
                    "senses": [{
                      "glosses": ["showing great attention to detail"],
                      "examples": [{"text": "He is meticulous in his work."}]
                    }]
                  }]
                }
                """)));
        when(cacheService.getOrFetch(eq(ExternalApiCache.Provider.WIKTAPI), startsWith("pronunciations:"),
                anyString(), any(Duration.class), any())).thenReturn(Optional.of(read("""
                {
                  "pronunciations": [{
                    "sounds": [{"ipa": "/məˈtɪkjələs/", "audio": "//audio.example/meticulous.mp3"}]
                  }]
                }
                """)));
        when(cacheService.getOrFetch(eq(ExternalApiCache.Provider.WIKTAPI), startsWith("translations:"),
                anyString(), any(Duration.class), any())).thenReturn(Optional.of(read("{\"translations\":[]}")));

        WiktApiClient client = new WiktApiClient(restTemplate, objectMapper, properties, cacheService);

        ProviderWordData data = client.lookupEnglish("Meticulous");

        assertThat(data.getPartOfSpeech()).isEqualTo("adjective");
        assertThat(data.getDefinition()).isEqualTo("showing great attention to detail");
        assertThat(data.getExample()).isEqualTo("He is meticulous in his work.");
        assertThat(data.getPronunciation()).isEqualTo("/məˈtɪkjələs/");
        assertThat(data.getAudioUrl()).isEqualTo("https://audio.example/meticulous.mp3");
        assertThat(data.getRawJson()).isNotNull();
    }

    @Test
    void pixabayClientMapsFirstHitToImageCandidate() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ExternalApiCacheService cacheService = mock(ExternalApiCacheService.class);
        EnrichmentProperties properties = new EnrichmentProperties();
        properties.getApi().getPixabay().setBaseUrl("https://pixabay.com/api/");
        properties.getApi().getPixabay().setKey("pixabay-key");

        when(cacheService.getOrFetch(eq(ExternalApiCache.Provider.PIXABAY), eq("photo:meticulous"),
                anyString(), any(Duration.class), any())).thenReturn(Optional.of(read("""
                {
                  "hits": [{
                    "id": 123,
                    "largeImageURL": "https://cdn.example/meticulous.jpg",
                    "pageURL": "https://pixabay.com/photos/123",
                    "user": "Alice"
                  }]
                }
                """)));

        PixabayImageClient client = new PixabayImageClient(restTemplate, objectMapper, properties, cacheService);

        ImageCandidate candidate = client.searchFirstPhoto("meticulous").orElseThrow();

        assertThat(candidate.getProvider()).isEqualTo("PIXABAY");
        assertThat(candidate.getProviderImageId()).isEqualTo("123");
        assertThat(candidate.getDownloadUrl()).isEqualTo("https://cdn.example/meticulous.jpg");
        assertThat(candidate.getPageUrl()).isEqualTo("https://pixabay.com/photos/123");
        assertThat(candidate.getAttribution()).isEqualTo("Image by Alice on Pixabay");
    }

    @Test
    void pexelsClientMapsFirstPhotoToImageCandidate() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ExternalApiCacheService cacheService = mock(ExternalApiCacheService.class);
        EnrichmentProperties properties = new EnrichmentProperties();
        properties.getApi().getPexels().setBaseUrl("https://api.pexels.com/v1");
        properties.getApi().getPexels().setKey("pexels-key");

        when(cacheService.getOrFetch(eq(ExternalApiCache.Provider.PEXELS), eq("photo:meticulous"),
                anyString(), any(Duration.class), any())).thenReturn(Optional.of(read("""
                {
                  "photos": [{
                    "id": 456,
                    "url": "https://pexels.com/photo/456",
                    "photographer": "Bao Nguyen",
                    "src": { "large": "https://images.pexels.com/photos/456/large.jpg" }
                  }]
                }
                """)));

        PexelsImageClient client = new PexelsImageClient(restTemplate, objectMapper, properties, cacheService);

        ImageCandidate candidate = client.searchFirstPhoto("meticulous").orElseThrow();

        assertThat(candidate.getProvider()).isEqualTo("PEXELS");
        assertThat(candidate.getProviderImageId()).isEqualTo("456");
        assertThat(candidate.getDownloadUrl()).isEqualTo("https://images.pexels.com/photos/456/large.jpg");
        assertThat(candidate.getPageUrl()).isEqualTo("https://pexels.com/photo/456");
        assertThat(candidate.getAttribution()).isEqualTo("Photo by Bao Nguyen on Pexels");
    }

    private JsonNode read(String json) throws Exception {
        return objectMapper.readTree(json);
    }
}
