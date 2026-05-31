package com.fsrspring.vocab.controller;

import com.fsrspring.vocab.model.AppUser;
import com.fsrspring.vocab.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> listUsers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<AppUser> result = adminUserService.listUsers(email, status, role, from, to, page, size);
        List<Map<String, Object>> content = result.getContent().stream()
                .map(adminUserService::buildUserDto).toList();

        return ResponseEntity.ok(Map.of(
                "users", content,
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "page", result.getNumber(),
                "size", result.getSize()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUserDetail(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getUserDetail(id));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<Map<String, Object>>> getUserHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(adminUserService.getUserHistory(id, limit));
    }

    @PutMapping("/{id}/lock")
    public ResponseEntity<Map<String, Object>> lockUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.lockUser(id));
    }

    @PutMapping("/{id}/unlock")
    public ResponseEntity<Map<String, Object>> unlockUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.unlockUser(id));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.resetPassword(id));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<Map<String, Object>> assignRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String role = body.get("role");
        if (role == null || role.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "role is required"));
        }
        return ResponseEntity.ok(adminUserService.assignRole(id, role));
    }
}
