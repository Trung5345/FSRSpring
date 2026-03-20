package com.fsrspring.vocab.service;

import com.fsrspring.vocab.model.ReviewEvent;
import com.fsrspring.vocab.model.UserProgress;
import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.repository.ReviewEventRepository;
import com.fsrspring.vocab.repository.UserProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class FsrsService {

    private final UserProgressRepository progressRepository;
    private final ReviewEventRepository reviewEventRepository;

    public UserProgress getOrCreateProgress(Word word) {
        return progressRepository.findByWord(word)
                .orElseGet(() -> progressRepository.save(UserProgress.builder().word(word).build()));
    }

    public UserProgress reviewWord(Word word, int rating, long responseTimeMs) {
        int boundedRating = Math.max(1, Math.min(4, rating));
        UserProgress progress = getOrCreateProgress(word);
        LocalDateTime now = LocalDateTime.now();

        boolean correct = boundedRating >= 3;
        if (correct) {
            progress.setCorrectCount(progress.getCorrectCount() + 1);
            progress.setFsrsRepetition(progress.getFsrsRepetition() + 1);
        } else {
            progress.setIncorrectCount(progress.getIncorrectCount() + 1);
            progress.setFsrsLapseCount(progress.getFsrsLapseCount() + 1);
            progress.setFsrsRepetition(0);
        }

        Duration gap = progress.getLastStudied() == null
                ? Duration.ofHours(0)
                : Duration.between(progress.getLastStudied(), now);

        double lastIntervalHours = Math.max(0.0, gap.toMinutes() / 60.0);
        progress.setLastIntervalHours(lastIntervalHours);
        progress.setLastStudied(now);

        updateSequentialState(progress, boundedRating, responseTimeMs);
        updateFsrsState(progress, boundedRating, lastIntervalHours);

        progress.setMastery(calculateMastery(progress));
        progress.setNextReview(calculateNextReview(now, progress, boundedRating));

        reviewEventRepository.save(ReviewEvent.builder()
                .word(word)
                .rating(boundedRating)
                .correct(correct)
                .responseTimeMs(Math.max(0L, responseTimeMs))
                .reviewedAt(now)
                .build());

        return progressRepository.save(progress);
    }

    public List<UserProgress> getDueWords(int limit) {
        List<UserProgress> due = progressRepository.findDueWords(LocalDateTime.now());
        if (limit > 0 && due.size() > limit) {
            return due.subList(0, limit);
        }
        return due;
    }

    public Map<String, Object> getFsrsStats() {
        LocalDateTime now = LocalDateTime.now();
        long dueNow = progressRepository.countDueWords(now);
        long mastered = progressRepository.countMastered();
        long learning = progressRepository.countLearning();

        double retentionEstimate = 0.0;
        List<UserProgress> all = progressRepository.findAll();
        if (!all.isEmpty()) {
            retentionEstimate = all.stream()
                    .mapToDouble(UserProgress::getFsrsRetrievability)
                    .average()
                    .orElse(0.0) * 100.0;
        }

        return Map.of(
                "dueNow", dueNow,
                "mastered", mastered,
                "learning", learning,
                "retentionEstimate", Math.round(retentionEstimate * 10.0) / 10.0
        );
    }

    private void updateSequentialState(UserProgress progress, int rating, long responseTimeMs) {
        final double alpha = 0.2;

        progress.setSequenceStep(progress.getSequenceStep() + 1);

        double accuracyInput = rating >= 3 ? 1.0 : 0.0;
        double prevAcc = progress.getSequenceAccuracyEMA();
        progress.setSequenceAccuracyEMA(prevAcc == 0.0 ? accuracyInput : prevAcc * (1 - alpha) + accuracyInput * alpha);

        double responseInput = Math.max(0L, responseTimeMs);
        double prevResp = progress.getSequenceResponseMsEMA();
        progress.setSequenceResponseMsEMA(prevResp == 0.0 ? responseInput : prevResp * (1 - alpha) + responseInput * alpha);

        double consistencyInput = rating >= 3 ? 1.0 : -1.0;
        double prevConsistency = progress.getSequenceConsistency();
        progress.setSequenceConsistency(prevConsistency * (1 - alpha) + consistencyInput * alpha);

        double difficultyTrendInput = 5.0 - rating;
        double prevTrend = progress.getSequenceDifficultyTrend();
        progress.setSequenceDifficultyTrend(prevTrend * (1 - alpha) + difficultyTrendInput * alpha);
    }

    private void updateFsrsState(UserProgress progress, int rating, double lastIntervalHours) {
        double difficulty = progress.getFsrsDifficulty();
        difficulty = Math.max(1.0, Math.min(10.0, difficulty + (3.0 - rating) * 0.35));
        progress.setFsrsDifficulty(difficulty);

        double stability = progress.getFsrsStability();
        if (rating == 1) {
            stability = Math.max(0.15, stability * 0.65);
        } else {
            double gain = 0.12 + (rating - 2) * 0.08 + progress.getSequenceAccuracyEMA() * 0.15;
            stability = Math.min(365.0, stability * (1.0 + gain));
        }
        progress.setFsrsStability(stability);

        double retrievability = Math.exp(-Math.max(0.0, lastIntervalHours) / Math.max(1.0, stability * 24.0));
        progress.setFsrsRetrievability(Math.max(0.0, Math.min(1.0, retrievability)));
    }

    private LocalDateTime calculateNextReview(LocalDateTime now, UserProgress progress, int rating) {
        double baseHours = switch (rating) {
            case 1 -> 0.5;
            case 2 -> 6.0;
            case 3 -> 24.0;
            default -> 72.0;
        };

        double behaviorMultiplier = 1.0
                + (progress.getSequenceAccuracyEMA() * 0.4)
                - (Math.max(0.0, progress.getSequenceResponseMsEMA() - 3500.0) / 10000.0)
                + (progress.getSequenceConsistency() * 0.2);

        behaviorMultiplier = Math.max(0.4, Math.min(2.2, behaviorMultiplier));

        long nextHours = Math.max(1L, Math.round(baseHours * progress.getFsrsStability() * behaviorMultiplier));
        return now.plusHours(nextHours);
    }

    private UserProgress.MasteryLevel calculateMastery(UserProgress progress) {
        int correct = progress.getCorrectCount();
        double accuracy = progress.getAccuracy();

        if (correct == 0) {
            return UserProgress.MasteryLevel.NEW;
        }
        if (correct < 3 || accuracy < 60 || progress.getFsrsRetrievability() < 0.4) {
            return UserProgress.MasteryLevel.LEARNING;
        }
        if (correct < 8 || accuracy < 80 || progress.getFsrsRetrievability() < 0.75) {
            return UserProgress.MasteryLevel.REVIEWING;
        }
        return UserProgress.MasteryLevel.MASTERED;
    }
}
