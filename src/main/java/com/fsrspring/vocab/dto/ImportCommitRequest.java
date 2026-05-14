package com.fsrspring.vocab.dto;

import com.fsrspring.vocab.model.CefrLevel;
import com.fsrspring.vocab.model.Word;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ImportCommitRequest {

    private String sourceType;
    private String fileName;
    private TargetSet targetSet;
    private String targetSetName;
    private Map<String, Object> options;
    private List<Row> rows = new ArrayList<>();

    @Data
    public static class TargetSet {
        private Long id;
        private String name;
        private String description;
        private Long topicId;
        private CefrLevel cefrLevel;
    }

    @Data
    public static class Row {
        private String clientRowId;
        private String word;
        private String translation;
        private String example;
        private String pronunciation;
        private String category;
        private Word.DifficultyLevel difficulty;
        private Long topicId;
        private CefrLevel cefrLevel;
        private String partOfSpeech;
        private String audioUrl;
    }
}
