package com.fsrspring.vocab.controller;

import com.fsrspring.vocab.model.WordEnrichmentJob;
import com.fsrspring.vocab.service.LibreTranslateClient;
import com.fsrspring.vocab.service.WordEnrichmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/enrichment")
@RequiredArgsConstructor
public class EnrichmentController {

    private final WordEnrichmentService enrichmentService;
    private final LibreTranslateClient libreTranslateClient;

    @PostMapping("/words/{id}")
    public ResponseEntity<Map<String, Object>> enqueue(@PathVariable Long id) {
        return ResponseEntity.accepted().body(toBody(enrichmentService.retryWord(id)));
    }

    @GetMapping("/words/{id}")
    public ResponseEntity<Map<String, Object>> status(@PathVariable Long id) {
        return enrichmentService.latestJob(id)
                .map(job -> ResponseEntity.ok(toBody(job)))
                .orElseThrow(() -> new NoSuchElementException("No enrichment job found for word: " + id));
    }

    @PostMapping("/translate")
    public Map<String, Object> translateRows(@RequestBody Map<String, Object> payload) {
        Object rawRows = payload.get("rows");
        if (!(rawRows instanceof Iterable<?> rows)) {
            return Map.of("rows", java.util.List.of());
        }
        java.util.List<Map<String, Object>> translated = new java.util.ArrayList<>();
        for (Object rawRow : rows) {
            if (!(rawRow instanceof Map<?, ?> inputRow)) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            inputRow.forEach((key, value) -> row.put(String.valueOf(key), value));
            String word = text(row.get("word"));
            String translation = text(row.get("translation"));
            if (word != null && translation == null) {
                libreTranslateClient.translateEnToVi(word).ifPresent(value -> {
                    row.put("translation", value);
                    row.put("aiTranslated", true);
                    row.put("translationProvider", "LIBRE_TRANSLATE");
                });
            }
            translated.add(row);
        }
        return Map.of("rows", translated);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    private Map<String, Object> toBody(WordEnrichmentJob job) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jobId", job.getId());
        body.put("wordId", job.getWord() != null ? job.getWord().getId() : null);
        body.put("status", job.getStatus());
        body.put("attempts", job.getAttempts());
        body.put("nextRunAt", job.getNextRunAt());
        body.put("startedAt", job.getStartedAt());
        body.put("completedAt", job.getCompletedAt());
        body.put("lastError", job.getLastError());
        return body;
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
