package com.fsrspring.vocab.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProviderWordData {
    private String translation;
    private String pronunciation;
    private String audioUrl;
    private String partOfSpeech;
    private String example;
    private String definition;
    private String synonyms;
    private String antonyms;
    private String origin;
    private JsonNode rawJson;
}
