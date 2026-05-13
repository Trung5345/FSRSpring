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
        return "forward:/import.html";
    }

    @GetMapping("/quiz")
    public String quiz() {
        return "forward:/quiz.html";
    }

    @GetMapping("/progress")
    public String progress() {
        return "forward:/progress.html";
    }

    @GetMapping("/flashcards")
    public String flashcards() {
        return "forward:/flashcards.html";
    }

    @GetMapping("/profile")
    public String profile() {
        return "forward:/profile.html";
    }
}
