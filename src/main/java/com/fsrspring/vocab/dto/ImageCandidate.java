package com.fsrspring.vocab.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImageCandidate {
    private String provider;
    private String providerImageId;
    private String downloadUrl;
    private String pageUrl;
    private String attribution;
    private String metadataJson;
}
