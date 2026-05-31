package com.fsrspring.vocab.controller;

import com.fsrspring.vocab.model.Topic;
import com.fsrspring.vocab.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @GetMapping
    public List<Topic> getAll() {
        return topicService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Topic> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(topicService.getById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<Topic> getBySlug(@PathVariable String slug) {
        return topicService.getBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Topic> create(@RequestBody Topic topic) {
        try {
            return ResponseEntity.ok(topicService.create(topic));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Topic> update(@PathVariable Long id, @RequestBody Topic patch) {
        try {
            return ResponseEntity.ok(topicService.update(id, patch));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            topicService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
