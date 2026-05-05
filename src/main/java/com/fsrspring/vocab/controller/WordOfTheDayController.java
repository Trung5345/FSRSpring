package com.fsrspring.vocab.controller;

import com.fsrspring.vocab.model.WordOfTheDay;
import com.fsrspring.vocab.service.WordOfTheDayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/word-of-the-day")
@RequiredArgsConstructor
public class WordOfTheDayController {

    private final WordOfTheDayService wordOfTheDayService;

    @GetMapping
    public ResponseEntity<WordOfTheDay> getToday() {
        try {
            return ResponseEntity.ok(wordOfTheDayService.getTodaysWord());
        } catch (IllegalStateException e) {
            return ResponseEntity.noContent().build();
        }
    }
}
