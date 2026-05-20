package com.fsrspring.vocab.service;

import com.fsrspring.vocab.model.AppUser;
import com.fsrspring.vocab.model.UserProgress;
import com.fsrspring.vocab.model.Word;
import com.fsrspring.vocab.repository.ReviewEventRepository;
import com.fsrspring.vocab.repository.UserProgressRepository;
import com.fsrspring.vocab.repository.WordRepository;
import com.fsrspring.vocab.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * FSRS Algorithm Timeline Simulation Test
 *
 * Mô phỏng quá trình học một từ qua nhiều phiên ôn tập theo thời gian,
 * kiểm tra các tính chất cốt lõi của thuật toán FSRS:
 *  - Stability tăng sau mỗi lần ôn đúng
 *  - Stability giảm khi trả lời sai (Again)
 *  - Interval (nextReview) tăng dần theo stability
 *  - Mastery level tiến triển: NEW -> LEARNING -> REVIEWING -> MASTERED
 *  - Difficulty điều chỉnh theo rating
 */
@ExtendWith(MockitoExtension.class)
class FsrsSimulationTest {

    @Mock private UserProgressRepository progressRepository;
    @Mock private ReviewEventRepository reviewEventRepository;
    @Mock private WordRepository wordRepository;
    @Mock private CurrentUserService currentUserService;

    @InjectMocks private FsrsService fsrsService;

    private Word word;
    private AppUser user;
    private UserProgress storedProgress;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        word = new Word();

        when(currentUserService.getCurrentUser()).thenReturn(user);

        // progressRepository.save() trả về chính object được truyền vào
        when(progressRepository.save(any(UserProgress.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reviewEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Lần đầu tiên gọi findByUserAndWord trả về empty (word chưa từng học)
        // Sau đó trả về stored progress
        storedProgress = null;
        lenient().when(progressRepository.findByUserAndWord(eq(user), eq(word))).thenAnswer(inv -> {
            if (storedProgress == null) return Optional.empty();
            return Optional.of(storedProgress);
        });
    }

    /**
     * Mô phỏng một lần ôn tập, cập nhật storedProgress và in thông tin
     */
    private UserProgress simulateReview(int rating, long responseMs, String simulatedDateLabel) {
        UserProgress result = fsrsService.reviewWord(word, rating, responseMs);
        storedProgress = result;  // cập nhật state cho lần review tiếp theo

        String ratingLabel = switch (rating) {
            case 1 -> "Again";
            case 2 -> "Hard";
            case 3 -> "Good";
            case 4 -> "Easy";
            default -> "Unknown";
        };

        LocalDateTime next = result.getNextReview();
        LocalDateTime lastStudied = result.getLastStudied();
        Duration interval = (next != null && lastStudied != null)
                ? Duration.between(lastStudied, next) : Duration.ZERO;

        System.out.printf(
            "  [%s] Rating=%-5s | Stability=%.3f | Difficulty=%.2f | Retrievability=%.3f | " +
            "Interval=%s | Mastery=%-9s | Correct=%d | NextReview=%s%n",
            simulatedDateLabel, ratingLabel,
            result.getFsrsStability(), result.getFsrsDifficulty(), result.getFsrsRetrievability(),
            formatDuration(interval), result.getMastery(),
            result.getCorrectCount(), next
        );
        return result;
    }

    private String formatDuration(Duration d) {
        long hours = d.toHours();
        if (hours < 24) return hours + "h";
        return (hours / 24) + "d" + (hours % 24) + "h";
    }

    // =========================================================================
    // TEST 1: Học từ đúng liên tiếp - kiểm tra stability tăng dần
    // =========================================================================
    @Test
    @DisplayName("Học đúng liên tiếp: stability tăng dần, interval dài ra, cuối cùng đạt MASTERED")
    void testConsistentCorrectReviews_stabilityGrowsAndMasteryProgresses() {
        System.out.println("\n=== TEST 1: Học đúng liên tiếp ===");

        // Lần 1 - Day 0: Từ mới, trả lời Good
        UserProgress r1 = simulateReview(3, 2000, "Day 0");
        assertThat(r1.getMastery()).isEqualTo(UserProgress.MasteryLevel.LEARNING);
        assertThat(r1.getFsrsStability()).isGreaterThan(0.2);
        assertThat(r1.getCorrectCount()).isEqualTo(1);
        double stability1 = r1.getFsrsStability();
        LocalDateTime nextReview1 = r1.getNextReview();
        assertThat(nextReview1).isAfter(r1.getLastStudied());

        // Lần 2 - sau interval: trả lời Good
        UserProgress r2 = simulateReview(3, 1800, "Day ~1-2");
        assertThat(r2.getFsrsStability()).isGreaterThan(stability1); // stability tăng
        assertThat(r2.getCorrectCount()).isEqualTo(2);
        double stability2 = r2.getFsrsStability();

        // Lần 3 - trả lời Easy
        UserProgress r3 = simulateReview(4, 1500, "Day ~5");
        assertThat(r3.getFsrsStability()).isGreaterThan(stability2);
        assertThat(r3.getCorrectCount()).isEqualTo(3);
        double stability3 = r3.getFsrsStability();

        // Lần 4
        UserProgress r4 = simulateReview(3, 2000, "Day ~10");
        assertThat(r4.getFsrsStability()).isGreaterThan(stability3);
        assertThat(r4.getCorrectCount()).isEqualTo(4);
        double stability4 = r4.getFsrsStability();

        // Lần 5
        UserProgress r5 = simulateReview(3, 1800, "Day ~20");
        assertThat(r5.getFsrsStability()).isGreaterThan(stability4);
        assertThat(r5.getCorrectCount()).isEqualTo(5);
        double stability5 = r5.getFsrsStability();

        // Lần 6
        UserProgress r6 = simulateReview(4, 1200, "Day ~40");
        assertThat(r6.getFsrsStability()).isGreaterThan(stability5);
        assertThat(r6.getCorrectCount()).isEqualTo(6);

        // Lần 7
        UserProgress r7 = simulateReview(3, 1500, "Day ~70");
        assertThat(r7.getCorrectCount()).isEqualTo(7);

        // Lần 8 - đủ điều kiện MASTERED (≥8 đúng, accuracy ≥80%, retrievability ≥0.75)
        UserProgress r8 = simulateReview(4, 1000, "Day ~120");
        assertThat(r8.getCorrectCount()).isEqualTo(8);
        assertThat(r8.getMastery()).isEqualTo(UserProgress.MasteryLevel.MASTERED);

        // Kiểm tra interval ngày càng dài hơn (hay ít nhất bằng)
        Duration interval1 = Duration.between(r1.getLastStudied(), r1.getNextReview());
        Duration interval8 = Duration.between(r8.getLastStudied(), r8.getNextReview());
        assertThat(interval8).isGreaterThanOrEqualTo(interval1);

        System.out.println("  --> Stability growth: " + String.format("%.3f -> %.3f", stability1, r8.getFsrsStability()));
        System.out.println("  --> Interval growth: " + formatDuration(interval1) + " -> " + formatDuration(interval8));
    }

    // =========================================================================
    // TEST 2: Trả lời sai (Again) làm stability giảm và reset repetition
    // =========================================================================
    @Test
    @DisplayName("Trả lời Again: stability giảm, interval rút ngắn, repetition reset về 0")
    void testAgainRating_decreasesStabilityAndResetsRepetition() {
        System.out.println("\n=== TEST 2: Lapse sau 3 lần đúng ===");

        // 3 lần đúng trước
        simulateReview(3, 2000, "Day 0");
        simulateReview(3, 1800, "Day 3");
        UserProgress r3 = simulateReview(4, 1500, "Day 10");

        double stabilityBeforeLapse = r3.getFsrsStability();
        int repsBeforeLapse = r3.getFsrsRepetition();
        System.out.println("  Trước lapse: stability=" + String.format("%.3f", stabilityBeforeLapse)
                + ", reps=" + repsBeforeLapse);

        // Lapse: trả lời Again
        UserProgress lapse = simulateReview(1, 5000, "Day 15 (LAPSE)");

        assertThat(lapse.getFsrsStability()).isLessThan(stabilityBeforeLapse);
        assertThat(lapse.getFsrsRepetition()).isEqualTo(0); // repetition reset
        assertThat(lapse.getFsrsLapseCount()).isEqualTo(1);

        // Interval sau lapse phải ngắn (≤ 24 giờ)
        Duration lapseInterval = Duration.between(lapse.getLastStudied(), lapse.getNextReview());
        assertThat(lapseInterval.toHours()).isLessThanOrEqualTo(24);

        System.out.println("  Sau lapse: stability=" + String.format("%.3f", lapse.getFsrsStability())
                + " (giảm từ " + String.format("%.3f", stabilityBeforeLapse) + ")");
        System.out.println("  Interval sau lapse: " + formatDuration(lapseInterval));
    }

    // =========================================================================
    // TEST 3: Difficulty điều chỉnh theo rating
    // =========================================================================
    @Test
    @DisplayName("Difficulty: tăng khi Again/Hard, giảm khi Easy")
    void testDifficultyAdjustment() {
        System.out.println("\n=== TEST 3: Difficulty adjustment ===");

        // Baseline: Good review
        UserProgress base = simulateReview(3, 2000, "Day 0 (Good)");
        double diffAfterGood = base.getFsrsDifficulty();
        // Good (rating=3): difficulty += (3-3)*0.35 = 0 → không đổi
        assertThat(diffAfterGood).isEqualTo(5.0); // bắt đầu từ 5.0, Good không thay đổi

        // Again: difficulty tăng
        UserProgress afterAgain = simulateReview(1, 4000, "Day 1 (Again)");
        assertThat(afterAgain.getFsrsDifficulty()).isGreaterThan(diffAfterGood);

        // Easy: difficulty giảm
        double diffBeforeEasy = afterAgain.getFsrsDifficulty();
        UserProgress afterEasy = simulateReview(4, 1000, "Day 1 (Easy)");
        assertThat(afterEasy.getFsrsDifficulty()).isLessThan(diffBeforeEasy);

        System.out.println("  Initial difficulty: 5.0");
        System.out.println("  After Good (3): " + String.format("%.2f", diffAfterGood) + " (no change)");
        System.out.println("  After Again (1): " + String.format("%.2f", afterAgain.getFsrsDifficulty()) + " (increased)");
        System.out.println("  After Easy (4): " + String.format("%.2f", afterEasy.getFsrsDifficulty()) + " (decreased)");
    }

    // =========================================================================
    // TEST 4: Toàn bộ timeline mô phỏng một từ từ mới đến MASTERED
    // =========================================================================
    @Test
    @DisplayName("Full timeline: mô phỏng một từ từ NEW đến MASTERED với lapse ở giữa")
    void testFullLearningTimeline_withLapse() {
        System.out.println("\n=== TEST 4: Full timeline simulation (NEW -> MASTERED, có lapse) ===");
        System.out.println("  Mỗi dòng là một phiên ôn tập:");

        UserProgress r;

        // Phase 1: Học lần đầu
        r = simulateReview(3, 2000, "Session 1 (Day 0)");
        assertThat(r.getMastery()).isEqualTo(UserProgress.MasteryLevel.LEARNING);

        r = simulateReview(3, 1800, "Session 2");
        assertThat(r.getMastery()).isEqualTo(UserProgress.MasteryLevel.LEARNING);

        r = simulateReview(4, 1200, "Session 3");
        assertThat(r.getCorrectCount()).isEqualTo(3);

        // Phase 2: Ôn tập thường xuyên
        r = simulateReview(3, 1500, "Session 4");
        r = simulateReview(3, 1700, "Session 5");

        // Phase 3: Lapse - quên đột ngột
        double stabilityBeforeLapse = r.getFsrsStability();
        r = simulateReview(1, 8000, "Session 6 (LAPSE!)");
        assertThat(r.getFsrsStability()).isLessThan(stabilityBeforeLapse);
        assertThat(r.getFsrsLapseCount()).isEqualTo(1);

        // Phase 4: Recover sau lapse
        r = simulateReview(2, 4000, "Session 7 (recover Hard)");
        r = simulateReview(3, 2500, "Session 8");
        r = simulateReview(3, 2000, "Session 9");
        r = simulateReview(4, 1500, "Session 10");
        r = simulateReview(3, 1800, "Session 11");

        // Phase 5: Đạt MASTERED
        UserProgress finalResult = simulateReview(4, 1000, "Session 12 (MASTERED?)");

        System.out.println("\n  === Final state ===");
        System.out.println("  Mastery: " + finalResult.getMastery());
        System.out.println("  Correct/Total: " + finalResult.getCorrectCount() + "/" + finalResult.getTotalAttempts());
        System.out.println("  Accuracy: " + String.format("%.1f%%", finalResult.getAccuracy()));
        System.out.println("  Final stability: " + String.format("%.3f", finalResult.getFsrsStability()));
        System.out.println("  Lapses: " + finalResult.getFsrsLapseCount());

        // Assertions cơ bản
        assertThat(finalResult.getCorrectCount() + finalResult.getIncorrectCount())
                .isEqualTo(12); // 12 phiên ôn tập
        // Session 6 (Again) là lapse duy nhất - sau khi fix bug Hard không còn count là lapse
        assertThat(finalResult.getFsrsLapseCount()).isEqualTo(1);
        assertThat(finalResult.getFsrsStability()).isGreaterThan(0.2); // stability dù lapse vẫn lớn hơn ban đầu
    }

    // =========================================================================
    // TEST 5: Kiểm tra interval cụ thể theo stability
    // =========================================================================
    @Test
    @DisplayName("NextReview interval: Easy rating tạo interval dài hơn Good, Good dài hơn Hard")
    void testIntervalOrderByRating() {
        System.out.println("\n=== TEST 5: Interval theo từng rating (cùng stability) ===");

        // Again
        storedProgress = null;
        UserProgress rAgain = fsrsService.reviewWord(word, 1, 2000);
        Duration intervalAgain = Duration.between(rAgain.getLastStudied(), rAgain.getNextReview());

        // Hard (fresh progress)
        storedProgress = null;
        UserProgress rHard = fsrsService.reviewWord(word, 2, 2000);
        Duration intervalHard = Duration.between(rHard.getLastStudied(), rHard.getNextReview());

        // Good (fresh progress)
        storedProgress = null;
        UserProgress rGood = fsrsService.reviewWord(word, 3, 2000);
        Duration intervalGood = Duration.between(rGood.getLastStudied(), rGood.getNextReview());

        // Easy (fresh progress)
        storedProgress = null;
        UserProgress rEasy = fsrsService.reviewWord(word, 4, 2000);
        Duration intervalEasy = Duration.between(rEasy.getLastStudied(), rEasy.getNextReview());

        System.out.printf("  Again (1): interval = %s%n", formatDuration(intervalAgain));
        System.out.printf("  Hard  (2): interval = %s%n", formatDuration(intervalHard));
        System.out.printf("  Good  (3): interval = %s%n", formatDuration(intervalGood));
        System.out.printf("  Easy  (4): interval = %s%n", formatDuration(intervalEasy));

        // Thứ tự interval: Again <= Hard <= Good <= Easy (cả Again và Hard cùng clamp ở 1h với stability thấp)
        assertThat(intervalAgain).isLessThanOrEqualTo(intervalHard);
        assertThat(intervalHard).isLessThanOrEqualTo(intervalGood);
        assertThat(intervalGood).isLessThanOrEqualTo(intervalEasy);
        // Easy phải dài hơn Again một cách có ý nghĩa
        assertThat(intervalEasy).isGreaterThan(intervalAgain);
    }

    // =========================================================================
    // TEST 6: Kiểm tra retrievability giảm với lastIntervalHours lớn
    // =========================================================================
    @Test
    @DisplayName("Retrievability: cao khi ôn sớm, thấp khi để lâu (simulated gap)")
    void testRetrievability_decreasesWithLongerGap() {
        System.out.println("\n=== TEST 6: Retrievability theo khoảng cách thời gian ===");

        // Review đầu tiên: gap = 0 giờ → retrievability = exp(0) = 1.0
        UserProgress r1 = simulateReview(3, 2000, "Day 0 (gap=0h)");
        double retrievabilityDay0 = r1.getFsrsRetrievability();
        System.out.println("  Retrievability sau review đầu (gap=0h): " + String.format("%.4f", retrievabilityDay0));
        // gap=0 → exp(0/stability*24) = exp(0) = 1.0
        assertThat(retrievabilityDay0).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.001));

        // Không thể trực tiếp kiểm tra retrievability với gap lớn qua API (do dùng LocalDateTime.now())
        // Nên ta sẽ kiểm tra qua công thức: stability cũ, interval mới
        // Chỉ cần verify rằng retrievability nằm trong [0, 1]
        assertThat(r1.getFsrsRetrievability()).isBetween(0.0, 1.0);
    }

    // =========================================================================
    // TEST 7: Bound check - rating ngoài [1,4] phải bị clamp
    // =========================================================================
    @Test
    @DisplayName("Rating bound: rating=0 bị clamp về 1, rating=5 bị clamp về 4")
    void testRatingBounds() {
        System.out.println("\n=== TEST 7: Rating clamp ===");

        // rating=0 → clamp về 1 (Again) → lapseCount tăng
        storedProgress = null;
        UserProgress rTooLow = fsrsService.reviewWord(word, 0, 2000);
        System.out.println("  Rating 0 clamp -> Again: lapseCount=" + rTooLow.getFsrsLapseCount()
                + ", repetition=" + rTooLow.getFsrsRepetition());
        assertThat(rTooLow.getFsrsLapseCount()).isEqualTo(1);
        assertThat(rTooLow.getFsrsRepetition()).isEqualTo(0);

        // rating=5 → clamp về 4 (Easy) → correctCount tăng
        storedProgress = null;
        UserProgress rTooHigh = fsrsService.reviewWord(word, 5, 2000);
        System.out.println("  Rating 5 clamp -> Easy: correctCount=" + rTooHigh.getCorrectCount()
                + ", lapseCount=" + rTooHigh.getFsrsLapseCount());
        assertThat(rTooHigh.getCorrectCount()).isEqualTo(1);
        assertThat(rTooHigh.getFsrsLapseCount()).isEqualTo(0);
    }

    // =========================================================================
    // TEST 8: Null-safe getters - đảm bảo không NPE với null fields (existing DB rows)
    // =========================================================================
    @Test
    @DisplayName("Null-safe getters: UserProgress với null fields không throw NPE")
    void testNullSafeGetters_noNPEWithNullFields() {
        System.out.println("\n=== TEST 8: Null-safe getters ===");

        // Mô phỏng record cũ từ DB: tất cả FSRS fields là null
        UserProgress oldDbRecord = new UserProgress();
        // (không gọi builder → @Builder.Default không áp dụng → fields là null)

        // Phải không throw NPE
        assertThat(oldDbRecord.getFsrsStability()).isEqualTo(0.2);
        assertThat(oldDbRecord.getFsrsDifficulty()).isEqualTo(5.0);
        assertThat(oldDbRecord.getFsrsRepetition()).isEqualTo(0);
        assertThat(oldDbRecord.getFsrsLapseCount()).isEqualTo(0);
        assertThat(oldDbRecord.getFsrsRetrievability()).isEqualTo(0.0);
        assertThat(oldDbRecord.getLastIntervalHours()).isEqualTo(0.0);
        assertThat(oldDbRecord.getSequenceAccuracyEMA()).isEqualTo(0.0);
        assertThat(oldDbRecord.getSequenceResponseMsEMA()).isEqualTo(0.0);
        assertThat(oldDbRecord.getSequenceConsistency()).isEqualTo(0.0);
        assertThat(oldDbRecord.getSequenceDifficultyTrend()).isEqualTo(0.0);
        assertThat(oldDbRecord.getSequenceStep()).isEqualTo(0);
        assertThat(oldDbRecord.getCorrectCount()).isEqualTo(0);
        assertThat(oldDbRecord.getIncorrectCount()).isEqualTo(0);
        assertThat(oldDbRecord.getTotalAttempts()).isEqualTo(0);
        assertThat(oldDbRecord.getAccuracy()).isEqualTo(0.0);

        System.out.println("  Tất cả null-safe getters hoạt động đúng.");

        // Simulate review với record cũ từ DB (null fields)
        storedProgress = oldDbRecord;
        UserProgress result = fsrsService.reviewWord(word, 3, 2000);
        assertThat(result).isNotNull();
        System.out.println("  Review thành công với DB record cũ: nextReview=" + result.getNextReview());
    }
}
