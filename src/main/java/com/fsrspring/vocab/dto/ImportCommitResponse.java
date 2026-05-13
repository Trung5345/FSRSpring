package com.fsrspring.vocab.dto;

import com.fsrspring.vocab.model.ImportJobRow;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ImportCommitResponse {
    private Long importJobId;
    private int created;
    private int skipped;
    private int failed;
    private List<RowResult> rows;

    @Data
    @Builder
    public static class RowResult {
        private String clientRowId;
        private String word;
        private ImportJobRow.Status status;
        private Long wordId;
        private String enrichmentStatus;
        private String message;
    }
}
