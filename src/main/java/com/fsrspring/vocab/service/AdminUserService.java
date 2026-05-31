package com.fsrspring.vocab.service;

import com.fsrspring.vocab.model.AppUser;
import com.fsrspring.vocab.model.Deck;
import com.fsrspring.vocab.model.ReviewEvent;
import com.fsrspring.vocab.model.UserStreak;
import com.fsrspring.vocab.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminUserService {

    private static final String CHARS = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    private final AppUserRepository appUserRepository;
    private final UserProgressRepository userProgressRepository;
    private final UserStreakRepository userStreakRepository;
    private final ReviewEventRepository reviewEventRepository;
    private final DeckRepository deckRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<AppUser> listUsers(String email, String status, String role, String from, String to, int page, int size) {
        Boolean locked = switch (status == null ? "" : status.toLowerCase()) {
            case "locked" -> true;
            case "active" -> false;
            default -> null;
        };

        AppUser.Role roleEnum = null;
        if (role != null && !role.isBlank()) {
            try {
                roleEnum = AppUser.Role.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        LocalDateTime fromDt = from != null && !from.isBlank() ? LocalDate.parse(from).atStartOfDay() : null;
        LocalDateTime toDt = to != null && !to.isBlank() ? LocalDate.parse(to).atTime(23, 59, 59) : null;

        String emailFilter = (email != null && !email.isBlank()) ? email.trim() : null;

        return appUserRepository.searchUsers(
                emailFilter, locked, roleEnum, fromDt, toDt,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserDetail(Long id) {
        AppUser user = getUser(id);

        long totalWords = userProgressRepository.findByUser(user).size();
        long masteredCount = userProgressRepository.countMastered(user);
        long learningCount = userProgressRepository.countLearning(user);
        Long correct = userProgressRepository.sumCorrectAnswers(user);
        Long incorrect = userProgressRepository.sumIncorrectAnswers(user);
        long totalAnswers = (correct != null ? correct : 0) + (incorrect != null ? incorrect : 0);
        double accuracy = totalAnswers > 0 ? (double)(correct != null ? correct : 0) / totalAnswers * 100 : 0;

        Optional<UserStreak> streak = userStreakRepository.findByUser(user);
        List<Deck> decks = deckRepository.findByUserId(id);

        return Map.of(
                "user", buildUserDto(user),
                "stats", Map.of(
                        "totalWords", totalWords,
                        "mastered", masteredCount,
                        "learning", learningCount,
                        "accuracy", Math.round(accuracy * 10.0) / 10.0,
                        "correctAnswers", correct != null ? correct : 0,
                        "incorrectAnswers", incorrect != null ? incorrect : 0
                ),
                "streak", streak.map(s -> Map.of(
                        "currentStreak", s.getCurrentStreak(),
                        "longestStreak", s.getLongestStreak(),
                        "totalDaysStudied", s.getTotalDaysStudied(),
                        "lastStudyDate", s.getLastStudyDate() != null ? s.getLastStudyDate().toString() : ""
                )).orElse(Map.of("currentStreak", 0, "longestStreak", 0, "totalDaysStudied", 0, "lastStudyDate", "")),
                "decks", decks.stream().map(d -> Map.of(
                        "id", d.getId(),
                        "name", d.getName(),
                        "description", d.getDescription() != null ? d.getDescription() : "",
                        "isPublic", d.getIsPublic() != null && d.getIsPublic(),
                        "createdAt", d.getCreatedAt() != null ? d.getCreatedAt().toString() : ""
                )).toList()
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserHistory(Long id, int limit) {
        AppUser user = getUser(id);
        List<ReviewEvent> events = reviewEventRepository.findByUserOrderByReviewedAtDesc(user, PageRequest.of(0, limit));
        return events.stream().map(e -> Map.<String, Object>of(
                "id", e.getId(),
                "word", e.getWord().getWord(),
                "rating", e.getRating(),
                "correct", e.getCorrect(),
                "reviewedAt", e.getReviewedAt().toString()
        )).toList();
    }

    public Map<String, Object> lockUser(Long id) {
        AppUser user = getUser(id);
        user.setLocked(true);
        appUserRepository.save(user);
        return Map.of("id", id, "locked", true);
    }

    public Map<String, Object> unlockUser(Long id) {
        AppUser user = getUser(id);
        user.setLocked(false);
        appUserRepository.save(user);
        return Map.of("id", id, "locked", false);
    }

    public Map<String, Object> resetPassword(Long id) {
        AppUser user = getUser(id);
        String tempPassword = generatePassword(10);
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        appUserRepository.save(user);
        return Map.of("id", id, "temporaryPassword", tempPassword);
    }

    public Map<String, Object> assignRole(Long id, String role) {
        AppUser user = getUser(id);
        AppUser.Role newRole;
        try {
            newRole = AppUser.Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + role);
        }
        user.setRole(newRole);
        appUserRepository.save(user);
        return Map.of("id", id, "role", newRole.name());
    }

    public Map<String, Object> buildUserDto(AppUser u) {
        return Map.of(
                "id", u.getId(),
                "email", u.getEmail(),
                "name", u.getName() != null ? u.getName() : "",
                "avatarUrl", u.getAvatarUrl() != null ? u.getAvatarUrl() : "",
                "role", u.getRole().name(),
                "locked", u.getLocked() != null && u.getLocked(),
                "createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : "",
                "lastLoginAt", u.getLastLoginAt() != null ? u.getLastLoginAt().toString() : ""
        );
    }

    private AppUser getUser(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));
    }

    private String generatePassword(int length) {
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(CHARS.charAt(rng.nextInt(CHARS.length())));
        return sb.toString();
    }
}
