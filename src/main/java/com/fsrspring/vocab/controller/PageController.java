package com.fsrspring.vocab.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }

    @GetMapping("/vocabulary")
    public String vocabulary() {
        return "forward:/vocabulary.html";
    }

    @GetMapping("/import")
    public String learn() {
        return "forward:/learn.html";
    }

    @GetMapping("/quiz")
    public String quiz() {
        return "forward:/quiz.html";
    }

    @GetMapping("/progress")
    public String progress() {
        return "forward:/progress.html";
    }

    @GetMapping("/content")
    public String content() {
        return "forward:/content.html";
    }

    @GetMapping("/flashcards")
    public String flashcards() {
        return "forward:/flashcards.html";
    }

    @GetMapping("/flashcard-study")
    public String flashcardStudy() {
        return "forward:/flashcard-study.html";
    }

    @GetMapping("/sets")
    public String sets() {
        return "forward:/sets.html";
    }
}
