package com.fsrspring.vocab.controller;

import com.fsrspring.vocab.dto.DictionaryApiResponse;
import com.fsrspring.vocab.service.WordEnrichmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dictionary")
@RequiredArgsConstructor
public class DictionaryController {

    private final WordEnrichmentService enrichmentService;

    /** Preview dictionary data for any word text (does not save to DB) */
    @GetMapping("/lookup/{word}")
    public ResponseEntity<DictionaryApiResponse> lookup(@PathVariable String word) {
        return enrichmentService.previewLookup(word)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Enrich a saved word entity by its ID */
    @PostMapping("/enrich/{id}")
    public ResponseEntity<Map<String, Object>> enrich(@PathVariable Long id) {
        return ResponseEntity.accepted().body(Map.of(
                "wordId", id,
                "status", enrichmentService.retryWord(id).getStatus(),
                "message", "Enrichment queued"
        ));
    }

    /** Bulk enrich all words missing enrichment data */
    @PostMapping("/enrich-all")
    public ResponseEntity<Map<String, Integer>> enrichAll() {
        int count = enrichmentService.enrichAllMissing();
        return ResponseEntity.ok(Map.of("enriched", count));
    }
}
