package com.fsrspring.vocab.controller;

import com.fsrspring.vocab.dto.ImportCommitRequest;
import com.fsrspring.vocab.dto.ImportCommitResponse;
import com.fsrspring.vocab.model.ImportJob;
import com.fsrspring.vocab.model.ImportJobRow;
import com.fsrspring.vocab.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    @PostMapping("/words/commit")
    public ResponseEntity<ImportCommitResponse> commitWords(@RequestBody ImportCommitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(importService.commitWords(request));
    }

    @GetMapping("/jobs")
    public List<Map<String, Object>> listJobs() {
        return importService.listJobs().stream()
                .map(this::toSummary)
                .toList();
    }

    @GetMapping("/jobs/{id}")
    public Map<String, Object> getJob(@PathVariable Long id) {
        ImportJob job = importService.getJob(id);
        Map<String, Object> body = toSummary(job);
        body.put("rows", job.getRows().stream().map(this::toRow).toList());
        return body;
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    private Map<String, Object> toSummary(ImportJob job) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", job.getId());
        body.put("sourceType", job.getSourceType());
        body.put("fileName", valueOrEmpty(job.getFileName()));
        body.put("targetSetId", valueOrEmpty(job.getTargetSetId()));
        body.put("targetSetName", valueOrEmpty(job.getTargetSetName()));
        body.put("status", job.getStatus());
        body.put("totalRows", job.getTotalRows());
        body.put("created", job.getCreatedCount());
        body.put("skipped", job.getSkippedCount());
        body.put("failed", job.getFailedCount());
        body.put("createdAt", job.getCreatedAt());
        return body;
    }

    private Map<String, Object> toRow(ImportJobRow row) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("rowNumber", row.getRowNumber());
        body.put("clientRowId", valueOrEmpty(row.getClientRowId()));
        body.put("word", row.getWordText());
        body.put("translation", valueOrEmpty(row.getTranslation()));
        body.put("status", row.getStatus());
        body.put("wordId", valueOrEmpty(row.getWordId()));
        body.put("message", valueOrEmpty(row.getMessage()));
        return body;
    }

    private Object valueOrEmpty(Object value) {
        return value != null ? value : "";
    }
}
