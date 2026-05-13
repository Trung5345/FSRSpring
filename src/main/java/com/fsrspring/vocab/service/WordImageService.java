package com.fsrspring.vocab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fsrspring.vocab.dto.ImageCandidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WordImageService {

    private final PixabayImageClient pixabayImageClient;
    private final PexelsImageClient pexelsImageClient;
    private final ImageStorageService imageStorageService;
    private final ObjectMapper objectMapper;

    public Optional<ImageResult> findAndStoreImage(String word) {
        Optional<ImageCandidate> candidate = pixabayImageClient.searchFirstPhoto(word)
                .or(() -> pexelsImageClient.searchFirstPhoto(word));
        if (candidate.isEmpty()) {
            return Optional.empty();
        }
        ImageStorageService.StoredImage stored = imageStorageService.downloadConvertAndStore(word, candidate.get());
        return Optional.of(new ImageResult(stored.publicUrl(), metadata(candidate.get(), stored)));
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

    public record ImageResult(String imageUrl, String metadataJson) {
    }
}
