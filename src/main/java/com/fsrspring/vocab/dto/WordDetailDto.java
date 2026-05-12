package com.fsrspring.vocab.dto;

import com.fsrspring.vocab.model.CefrLevel;
import com.fsrspring.vocab.model.Word;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Rich word detail DTO — merges Word entity + API data for frontend display
 */
@Data
@Builder
public class WordDetailDto {
    private Long id;
    private String word;
    private String translation;
    private String pronunciation;
    private String audioUrl;
    private String partOfSpeech;
    private String category;
    private Word.DifficultyLevel difficulty;
    private CefrLevel cefrLevel;
    private String topicName;
    private String topicSlug;
    private String topicEmoji;
    private String topicColor;
    private String synonyms;
    private String antonyms;
    private String origin;
    private String example;
    private String imageUrl;
    private List<MeaningDto> meanings;

    @Data
    @Builder
    public static class MeaningDto {
        private String partOfSpeech;
        private String definition;
        private String example;
    }
}
