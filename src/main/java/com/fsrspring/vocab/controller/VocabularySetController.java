package com.fsrspring.vocab.controller;

import com.fsrspring.vocab.model.CefrLevel;
import com.fsrspring.vocab.model.VocabularySet;
import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.service.VocabularySetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sets")
@RequiredArgsConstructor
public class VocabularySetController {

    private final VocabularySetService setService;

    @GetMapping
    public List<VocabularySet> getAll(@RequestParam(required = false) Long topicId,
                                      @RequestParam(required = false) CefrLevel cefrLevel) {
        if (topicId != null) return setService.getByTopic(topicId);
        if (cefrLevel != null) return setService.getByCefrLevel(cefrLevel);
        return setService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<VocabularySet> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(setService.getById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public VocabularySet createSet(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        Long topicId = body.get("topicId") != null ? Long.valueOf(body.get("topicId").toString()) : null;
        CefrLevel cefrLevel = body.get("cefrLevel") != null
                ? CefrLevel.valueOf(body.get("cefrLevel").toString()) : null;
        return setService.createSet(name, description, topicId, cefrLevel);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSet(@PathVariable Long id) {
        setService.deleteSet(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/words")
    public List<Word> getWords(@PathVariable Long id) {
        return setService.getWordsInSet(id);
    }

    @PostMapping("/{id}/words/{wordId}")
    public ResponseEntity<VocabularySet> addWord(@PathVariable Long id, @PathVariable Long wordId) {
        try {
            return ResponseEntity.ok(setService.addWord(id, wordId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/words/{wordId}")
    public ResponseEntity<VocabularySet> removeWord(@PathVariable Long id, @PathVariable Long wordId) {
        try {
            return ResponseEntity.ok(setService.removeWord(id, wordId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
