package com.fsrspring.vocab.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class EnrichmentProperties {

    private Api api = new Api();
    private Enrichment enrichment = new Enrichment();

    @Data
    public static class Api {
        private Provider libretranslate = new Provider();
        private Provider wiktapi = new Provider();
        private KeyedProvider pixabay = new KeyedProvider();
        private KeyedProvider pexels = new KeyedProvider();
    }

    @Data
    public static class Provider {
        private String baseUrl = "";
    }

    @Data
    public static class KeyedProvider extends Provider {
        private String key = "";
    }

    @Data
    public static class Enrichment {
        private boolean enabled = true;
        private String imageStorageDir = "data/enrichment-images";
        private String imagePublicPrefix = "/generated-images/";
        private long pollDelayMs = 10000L;
        private boolean storeImagesLocally = true;
    }
}
