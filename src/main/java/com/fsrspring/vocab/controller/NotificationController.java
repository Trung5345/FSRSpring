package com.fsrspring.vocab.controller;

import com.fsrspring.vocab.dto.ReminderSettingsRequest;
import com.fsrspring.vocab.dto.ReminderSettingsResponse;
import com.fsrspring.vocab.model.AppNotification;
import com.fsrspring.vocab.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<AppNotification>> list() {
        return ResponseEntity.ok(notificationService.listLatest());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        return ResponseEntity.ok(Map.of("unread", notificationService.unreadCount()));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<AppNotification> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @GetMapping("/settings")
    public ResponseEntity<ReminderSettingsResponse> settings() {
        return ResponseEntity.ok(notificationService.getReminderSettings());
    }

    @PutMapping("/settings")
    public ResponseEntity<ReminderSettingsResponse> updateSettings(@RequestBody ReminderSettingsRequest request) {
        return ResponseEntity.ok(notificationService.updateReminderSettings(request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }
}
