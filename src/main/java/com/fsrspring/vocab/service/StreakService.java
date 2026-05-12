package com.fsrspring.vocab.service;

import com.fsrspring.vocab.model.UserStreak;
import com.fsrspring.vocab.repository.UserStreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StreakService {

    private final UserStreakRepository streakRepository;

    /**
     * Returns the singleton streak record, creating it if it doesn't exist.
     */
    @Transactional
    public UserStreak getStreak() {
        List<UserStreak> all = streakRepository.findAll();
        if (!all.isEmpty()) {
            return all.get(0);
        }
        UserStreak fresh = UserStreak.builder().build();
        return streakRepository.save(fresh);
    }

    /**
     * Called when the user completes a study session.
     * Updates current streak, longest streak, total days.
     */
    @Transactional
    public UserStreak checkIn() {
        UserStreak streak = getStreak();
        LocalDate today = LocalDate.now();
        LocalDate last = streak.getLastStudyDate();

        if (last == null || last.isBefore(today.minusDays(1))) {
            // Streak broken or first check-in
            streak.setCurrentStreak(1);
        } else if (last.equals(today.minusDays(1))) {
            // Consecutive day
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
        }
        // Already checked in today — no change to streak count

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
