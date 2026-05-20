package com.fsrspring.vocab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fsrspring.vocab.config.EnrichmentProperties;
import com.fsrspring.vocab.dto.ImageCandidate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WordImageService {

    private final PixabayImageClient pixabayImageClient;
    private final PexelsImageClient pexelsImageClient;
    private final ImageStorageService imageStorageService;
    private final ObjectMapper objectMapper;
    private final EnrichmentProperties properties;

    public Optional<ImageResult> findAndStoreImage(String word) {
        Optional<ImageCandidate> candidate = pixabayImageClient.searchFirstPhoto(word)
                .or(() -> pexelsImageClient.searchFirstPhoto(word));

        if (candidate.isEmpty()) {
            log.debug("No image candidate found for word: {}", word);
            return Optional.empty();
        }

        // Trên Railway (storeImagesLocally=false): dùng CDN URL trực tiếp từ Pixabay/Pexels
        // → tránh mất ảnh khi restart vì Railway disk là ephemeral
        if (!properties.getEnrichment().isStoreImagesLocally()) {
            ImageCandidate c = candidate.get();
            String cdnUrl = c.getDownloadUrl();
            log.debug("Using CDN URL directly for '{}': {}", word, cdnUrl);
            return Optional.of(new ImageResult(cdnUrl, metadataCdn(c)));
        }

        // Local mode: download, convert, lưu vào disk
        try {
            ImageStorageService.StoredImage stored = imageStorageService.downloadConvertAndStore(word, candidate.get());
            return Optional.of(new ImageResult(stored.publicUrl(), metadata(candidate.get(), stored)));
        } catch (Exception e) {
            // Nếu lưu local thất bại, fallback sang CDN URL
            log.warn("Local image storage failed for '{}', falling back to CDN URL: {}", word, e.getMessage());
            ImageCandidate c = candidate.get();
            return Optional.of(new ImageResult(c.getDownloadUrl(), metadataCdn(c)));
        }
    }

    private String metadata(ImageCandidate candidate, ImageStorageService.StoredImage stored) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("provider", candidate.getProvider());
        node.put("providerImageId", candidate.getProviderImageId());
        node.put("pageUrl", candidate.getPageUrl());
        node.put("attribution", candidate.getAttribution());
        node.put("localUrl", stored.publicUrl());
        node.put("byteSize", stored.byteSize());
        node.put("sourceMetadataJson", candidate.getMetadataJson());
        return node.toString();
    }

    private String metadataCdn(ImageCandidate candidate) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("provider", candidate.getProvider());
        node.put("providerImageId", candidate.getProviderImageId());
        node.put("pageUrl", candidate.getPageUrl());
        node.put("attribution", candidate.getAttribution());
        node.put("cdnUrl", candidate.getDownloadUrl());
        node.put("sourceMetadataJson", candidate.getMetadataJson());
        return node.toString();
    }

    public record ImageResult(String imageUrl, String metadataJson) {
    }
}
