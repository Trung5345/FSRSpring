package com.fsrspring.vocab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Maps one word result from https://api.datamuse.com/words
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatamuseWordDto {
    private String word;
    private Integer score;
    private String[] tags;
}
