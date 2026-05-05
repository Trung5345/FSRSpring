package com.fsrspring.vocab.controller;

import com.fsrspring.vocab.model.UserStreak;
import com.fsrspring.vocab.service.StreakService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/streak")
@RequiredArgsConstructor
public class StreakController {

    private final StreakService streakService;

    @GetMapping
    public ResponseEntity<UserStreak> getStreak() {
        return ResponseEntity.ok(streakService.getStreak());
    }

    @PostMapping("/check-in")
    public ResponseEntity<UserStreak> checkIn() {
        return ResponseEntity.ok(streakService.checkIn());
    }
}
