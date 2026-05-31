package com.fsrspring.vocab.service;

import com.fsrspring.vocab.model.AppUser;
import com.fsrspring.vocab.model.UserStreak;
import com.fsrspring.vocab.repository.UserStreakRepository;
import com.fsrspring.vocab.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class StreakService {

    private final UserStreakRepository streakRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public UserStreak getStreak() {
        AppUser user = currentUserService.getCurrentUser();
        return streakRepository.findByUser(user)
                .orElseGet(() -> streakRepository.save(
                        UserStreak.builder().user(user).build()
                ));
    }

    @Transactional
    public UserStreak checkIn() {
        UserStreak streak = getStreak();
        LocalDate today = LocalDate.now();
        LocalDate last = streak.getLastStudyDate();

        if (last == null || last.isBefore(today.minusDays(1))) {
            streak.setCurrentStreak(1);
        } else if (last.equals(today.minusDays(1))) {
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
        }

        if (last == null || !last.equals(today)) {
            streak.setTotalDaysStudied(streak.getTotalDaysStudied() + 1);
        }

        if (streak.getCurrentStreak() > streak.getLongestStreak()) {
            streak.setLongestStreak(streak.getCurrentStreak());
        }

        streak.setLastStudyDate(today);
        return streakRepository.save(streak);
    }
}
