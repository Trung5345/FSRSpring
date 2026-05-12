package com.fsrspring.vocab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Maps response from https://api.dictionaryapi.dev/api/v2/entries/en/{word}
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DictionaryApiResponse {

    private String word;
    private String phonetic;
    private List<Phonetic> phonetics;
    private List<Meaning> meanings;
    private License license;
    private List<String> sourceUrls;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Phonetic {
        private String text;
        private String audio;
        private String sourceUrl;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meaning {
        private String partOfSpeech;
        private List<Definition> definitions;
        private List<String> synonyms;
        private List<String> antonyms;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Definition {
        private String definition;
        private String example;
        private List<String> synonyms;
        private List<String> antonyms;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class License {
        private String name;
        private String url;
    }

    /** Returns the first non-empty audio URL from phonetics list */
    public String getBestAudioUrl() {
        if (phonetics == null) return null;
        return phonetics.stream()
                .filter(p -> p.getAudio() != null && !p.getAudio().isBlank())
                .map(Phonetic::getAudio)
                .findFirst()
                .orElse(null);
    }

    /** Returns the IPA phonetic text */
    public String getBestPhonetic() {
        if (phonetic != null && !phonetic.isBlank()) return phonetic;
        if (phonetics == null) return null;
        return phonetics.stream()
                .filter(p -> p.getText() != null && !p.getText().isBlank())
                .map(Phonetic::getText)
                .findFirst()
                .orElse(null);
    }

    /** Returns first definition text */
    public String getFirstDefinition() {
        if (meanings == null || meanings.isEmpty()) return null;
        Meaning m = meanings.get(0);
        if (m.getDefinitions() == null || m.getDefinitions().isEmpty()) return null;
        return m.getDefinitions().get(0).getDefinition();
    }

    /** Returns first example */
    public String getFirstExample() {
        if (meanings == null) return null;
        for (Meaning m : meanings) {
            if (m.getDefinitions() == null) continue;
            for (Definition d : m.getDefinitions()) {
                if (d.getExample() != null && !d.getExample().isBlank()) return d.getExample();
            }
        }
        return null;
    }

    /** Returns part of speech of first meaning */
    public String getPrimaryPartOfSpeech() {
        if (meanings == null || meanings.isEmpty()) return null;
        return meanings.get(0).getPartOfSpeech();
    }

    /** Collects all synonyms (de-duped, comma-joined) */
    public String getAllSynonyms() {
        if (meanings == null) return null;
        return meanings.stream()
                .flatMap(m -> {
                    java.util.List<String> syns = new java.util.ArrayList<>();
                    if (m.getSynonyms() != null) syns.addAll(m.getSynonyms());
                    if (m.getDefinitions() != null) {
                        m.getDefinitions().forEach(d -> { if (d.getSynonyms() != null) syns.addAll(d.getSynonyms()); });
                    }
                    return syns.stream();
                })
                .distinct()
                .limit(10)
                .reduce((a, b) -> a + ", " + b)
                .orElse(null);
    }

    /** Collects all antonyms (de-duped, comma-joined) */
    public String getAllAntonyms() {
        if (meanings == null) return null;
        return meanings.stream()
                .flatMap(m -> {
                    java.util.List<String> ants = new java.util.ArrayList<>();
                    if (m.getAntonyms() != null) ants.addAll(m.getAntonyms());
                    if (m.getDefinitions() != null) {
                        m.getDefinitions().forEach(d -> { if (d.getAntonyms() != null) ants.addAll(d.getAntonyms()); });
                    }
                    return ants.stream();
                })
                .distinct()
                .limit(10)
                .reduce((a, b) -> a + ", " + b)
                .orElse(null);
    }
}
