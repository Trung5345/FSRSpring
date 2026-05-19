# Thuật toán FSRS trong FSRSpring

> **FSRS** = Free Spaced Repetition Scheduler — thuật toán lên lịch ôn tập thông minh dựa trên lý thuyết tâm lý học về trí nhớ.

---

## 1. Lý thuyết nền tảng

### 1.1. Đường cong quên lãng (Forgetting Curve)

Hermann Ebbinghaus (1885) phát hiện: bộ nhớ suy giảm theo hàm mũ theo thời gian nếu không ôn tập.

$$R(t) = e^{-t/S}$$

Trong đó:
- $R$ = **Retrievability** (khả năng nhớ lại), $R \in [0, 1]$
- $t$ = thời gian kể từ lần ôn tập gần nhất (tính bằng giờ)
- $S$ = **Stability** (độ bền của ký ức, tính bằng ngày)

**Ý nghĩa thực tiễn**: nếu $S = 10$ ngày, sau 10 ngày không ôn thì $R = e^{-1} \approx 0.37$, tức bạn chỉ còn ~37% khả năng nhớ.

### 1.2. Hiệu ứng Spacing

Ôn tập phân tán theo thời gian hiệu quả hơn nhồi nhét. Mỗi lần ôn thành công làm tăng $S$ — bộ nhớ bền hơn và cần ít ôn hơn.

### 1.3. Hiệu ứng Testing

Tự kiểm tra (recall) hiệu quả hơn đọc lại thụ động vì nó kích hoạt cơ chế phục hồi ký ức, từ đó củng cố trace bộ nhớ.

---

## 2. Mô hình dữ liệu — `UserProgress`

Mỗi cặp `(user, word)` lưu một bản ghi `UserProgress` chứa toàn bộ trạng thái FSRS:

```
src/main/java/com/fsrspring/vocab/model/UserProgress.java
```

### 2.1. Trạng thái FSRS cốt lõi

| Field | Kiểu | Default | Ý nghĩa |
|-------|------|---------|---------|
| `fsrsStability` | `Double` | `0.2` | Độ bền ký ức (ngày). Cao → nhớ lâu hơn |
| `fsrsDifficulty` | `Double` | `5.0` | Độ khó từ, thang `[1, 10]`. Cao → khó học hơn |
| `fsrsRepetition` | `Integer` | `0` | Số lần ôn thành công liên tiếp (reset về 0 khi quên) |
| `fsrsLapseCount` | `Integer` | `0` | Tổng số lần quên (rating = Again). Không bao giờ reset |
| `fsrsRetrievability` | `Double` | `0.0` | Xác suất nhớ lại hiện tại, $\in [0, 1]$ |
| `lastIntervalHours` | `Double` | `0.0` | Khoảng cách từ lần ôn trước đến lần này (giờ) |

### 2.2. Trạng thái tuần tự (Behavior Sequence)

Lấy cảm hứng từ RNN — theo dõi xu hướng hành vi của user theo thời gian:

| Field | Alpha EMA | Ý nghĩa |
|-------|-----------|---------|
| `sequenceAccuracyEMA` | 0.2 | Xu hướng độ chính xác gần đây: `1.0` khi Good/Easy, `0.0` khi Again/Hard |
| `sequenceResponseMsEMA` | 0.2 | Tốc độ phản hồi trung bình (ms). Thấp → nhớ nhanh |
| `sequenceConsistency` | 0.2 | Mức độ nhất quán: `+1` khi đúng, `-1` khi sai. Dao động `[-1, 1]` |
| `sequenceDifficultyTrend` | 0.2 | Xu hướng độ khó: `5 - rating`. Easy(4)→1.0, Again(1)→4.0 |
| `sequenceStep` | — | Tổng số bước ôn tập (đơn điệu tăng) |

> **EMA** (Exponential Moving Average): $\text{EMA}_t = \text{EMA}_{t-1} \cdot (1-\alpha) + x_t \cdot \alpha$
> 
> Với $\alpha = 0.2$, sự kiện gần nhất có trọng số 20%, dần fade out theo thời gian.

### 2.3. Trạng thái học tập

| Field | Ý nghĩa |
|-------|---------|
| `correctCount` | Tổng số lần trả lời đúng (Hard/Good/Easy) |
| `incorrectCount` | Tổng số lần trả lời sai (Again) |
| `mastery` | Enum: `NEW → LEARNING → REVIEWING → MASTERED` |
| `lastStudied` | Timestamp ôn lần cuối |
| `nextReview` | Timestamp được lên lịch ôn tiếp theo |

---

## 3. Hệ thống Rating

```java
// FsrsService.java — reviewWord()
int boundedRating = Math.max(1, Math.min(4, rating));
```

| Rating | Tên | Ý nghĩa FSRS | Tác động |
|--------|-----|--------------|---------|
| `1` | **Again** | Quên hoàn toàn (lapse) | Stability giảm, repetition reset = 0, lapseCount++ |
| `2` | **Hard** | Nhớ nhưng khó khăn | Stability tăng nhẹ, **KHÔNG** là lapse |
| `3` | **Good** | Nhớ bình thường | Stability tăng vừa |
| `4` | **Easy** | Nhớ rất dễ | Stability tăng mạnh |

**Quan trọng**: `correct = boundedRating >= 2` — Hard KHÔNG phải là lapse trong FSRS chuẩn. Chỉ `Again` (1) mới là lapse.

---

## 4. Luồng xử lý chính — `reviewWord()`

```java
public UserProgress reviewWord(Word word, int rating, long responseTimeMs)
```

```
reviewWord()
    │
    ├─ 1. Clamp rating về [1..4]
    ├─ 2. getOrCreateProgress() — load hoặc tạo mới UserProgress
    ├─ 3. Cập nhật correctCount / incorrectCount / repetition / lapseCount
    ├─ 4. Tính lastIntervalHours = gap từ lastStudied đến now
    ├─ 5. updateSequentialState() — cập nhật EMA behavior
    ├─ 6. updateFsrsState() — cập nhật stability, difficulty, retrievability
    ├─ 7. calculateMastery() — phân loại NEW/LEARNING/REVIEWING/MASTERED
    ├─ 8. calculateNextReview() — tính timestamp ôn tiếp theo
    ├─ 9. Lưu ReviewEvent (immutable audit log)
    └─ 10. Lưu và trả về UserProgress cập nhật
```

---

## 5. Cập nhật Trạng thái Tuần tự — `updateSequentialState()`

```java
private void updateSequentialState(UserProgress progress, int rating, long responseTimeMs)
```

### 5.1. Accuracy EMA

```java
double accuracyInput = rating >= 3 ? 1.0 : 0.0;
// 1.0 nếu Good/Easy; 0.0 nếu Again/Hard
newEMA = prevEMA * 0.8 + accuracyInput * 0.2;
```

- Phản ánh "xu hướng đúng gần đây"
- Sau 5 lần Good liên tiếp: $\approx 0.67$
- Sau 10 lần Good: $\approx 0.89$

### 5.2. Response Time EMA

```java
double responseInput = Math.max(0L, responseTimeMs);
newEMA = prevEMA * 0.8 + responseInput * 0.2;
```

- Theo dõi tốc độ phản hồi trung bình
- Dùng để phạt nếu user trả lời chậm (> 3500ms) trong bước tính interval

### 5.3. Consistency Score

```java
double consistencyInput = rating >= 3 ? 1.0 : -1.0;
// +1 khi đúng, -1 khi sai
newConsistency = prev * 0.8 + consistencyInput * 0.2;
```

- Dao động trong khoảng $(-1, +1)$
- Nhất quán đúng → tiệm cận +1, nhất quán sai → tiệm cận -1
- Dùng để thưởng consistency tốt trong bước tính interval

### 5.4. Difficulty Trend EMA

```java
double difficultyTrendInput = 5.0 - rating;
// Easy(4)→1.0, Good(3)→2.0, Hard(2)→3.0, Again(1)→4.0
newTrend = prev * 0.8 + difficultyTrendInput * 0.2;
```

- Theo dõi xu hướng khó khăn chủ quan của user với từ này
- Giá trị thấp → user thấy từ ngày càng dễ hơn

---

## 6. Cập nhật Trạng thái FSRS — `updateFsrsState()`

```java
private void updateFsrsState(UserProgress progress, int rating, double lastIntervalHours)
```

### 6.1. Cập nhật Difficulty

```java
difficulty = clamp(difficulty + (3 - rating) * 0.35, 1.0, 10.0)
```

**Bảng tác động:**

| Rating | Delta Difficulty | Ý nghĩa |
|--------|-----------------|---------|
| Again (1) | +0.70 | Từ khó hơn kỳ vọng |
| Hard (2) | +0.35 | Từ hơi khó |
| Good (3) | 0.00 | Không thay đổi |
| Easy (4) | −0.35 | Từ dễ hơn kỳ vọng |

Clamp vào `[1, 10]` — không bao giờ thoát ngoài biên.

### 6.2. Cập nhật Stability

**Trường hợp Lapse (Again = 1):**

```java
stability = max(0.15, stability * 0.65)
```

- Stability giảm còn 65% giá trị cũ
- Tối thiểu 0.15 ngày (~3.6 giờ) — không bao giờ về 0

**Trường hợp Correct (Hard/Good/Easy = 2/3/4):**

```java
double gain = 0.12 + (rating - 2) * 0.08 + sequenceAccuracyEMA * 0.15
stability = min(365.0, stability * (1 + gain))
```

**Bảng gain theo rating (khi accuracyEMA = 0.5):**

| Rating | `(rating-2)*0.08` | `accuracyEMA*0.15` | Total Gain | Multiplier |
|--------|-------------------|--------------------|------------|------------|
| Hard (2) | 0.00 | 0.075 | 0.195 | ×1.195 |
| Good (3) | 0.08 | 0.075 | 0.275 | ×1.275 |
| Easy (4) | 0.16 | 0.075 | 0.355 | ×1.355 |

Tối đa 365.0 ngày (1 năm) — ngưỡng "MASTERED tuyệt đối".

**Ví dụ tăng trưởng stability với Good liên tiếp (accuracyEMA ≈ 0.8):**

```
Review 1: S = 0.2  → 0.2 * (1 + 0.12 + 0 + 0.12) = 0.248 ngày  (~6h)
Review 2: S = 0.248 → ×1.36 ≈ 0.337 ngày  (~8h)
Review 3: S = 0.337 → ×1.36 ≈ 0.458 ngày  (~11h)
Review 5: S ≈ 0.85  ngày (~20h)
Review 8: S ≈ 3.5   ngày
Review 12: S ≈ 14   ngày
Review 16: S ≈ 58   ngày
Review 20: S ≈ 240  ngày
```

### 6.3. Cập nhật Retrievability

```java
double retrievability = exp(-lastIntervalHours / max(1.0, stability * 24.0))
retrievability = clamp(retrievability, 0.0, 1.0)
```

Đây chính là công thức đường cong quên lãng Ebbinghaus:

$$R = e^{-t/S_{hours}}$$

Trong đó $S_{hours} = S_{days} \times 24$, $t$ = `lastIntervalHours`.

**Ví dụ:**

| lastIntervalHours | stability (ngày) | Retrievability |
|-------------------|-----------------|----------------|
| 0h | 1.0 | 1.000 (vừa ôn xong) |
| 12h | 1.0 | 0.607 |
| 24h | 1.0 | 0.368 |
| 24h | 5.0 | 0.819 |
| 168h (7 ngày) | 10.0 | 0.496 |

---

## 7. Tính Thời gian Ôn Tiếp theo — `calculateNextReview()`

```java
private LocalDateTime calculateNextReview(LocalDateTime now, UserProgress progress, int rating)
```

### 7.1. Base Hours theo Rating

```java
double baseHours = switch (rating) {
    case 1 -> 0.5;    // Again:  30 phút
    case 2 -> 6.0;    // Hard:   6 giờ
    case 3 -> 24.0;   // Good:   1 ngày
    default -> 72.0;  // Easy:   3 ngày
};
```

### 7.2. Behavior Multiplier

```java
double behaviorMultiplier = 1.0
    + (sequenceAccuracyEMA  * 0.4)         // thưởng accuracy tốt
    - (max(0, responseMs - 3500) / 10000)  // phạt phản hồi chậm
    + (sequenceConsistency  * 0.2);        // thưởng nhất quán

behaviorMultiplier = clamp(behaviorMultiplier, 0.4, 2.2)
```

**Phân tích từng thành phần:**

| Thành phần | Tác động | Kịch bản |
|------------|---------|---------|
| `+accuracy * 0.4` | `[0, +0.4]` | User hay đúng → interval dài hơn 40% |
| `-slowPenalty` | `[0, -∞]` (capped) | User 5000ms → `-0.15`; 13500ms → `-1.0` |
| `+consistency * 0.2` | `[-0.2, +0.2]` | Đúng nhất quán → +20%, lộn xộn → -20% |

**Ví dụ:**
- User đạt accuracy EMA = 0.8, đáp 2000ms, consistency = 0.6:
  - mult = 1 + 0.32 - 0 + 0.12 = **1.44**
- User đạt accuracy EMA = 0.2, đáp 6000ms, consistency = -0.4:
  - mult = 1 + 0.08 - 0.25 - 0.08 = **0.75**

### 7.3. Công thức cuối cùng

```java
long nextHours = max(1L, round(baseHours * stability * behaviorMultiplier))
nextReview = now.plusHours(nextHours)
```

**Ví dụ tính interval:**

| Rating | baseHours | stability | mult | nextHours |
|--------|-----------|-----------|------|-----------|
| Good | 24h | 0.2 ngày | 1.0 | max(1, round(24 × 0.2 × 1.0)) = **5h** |
| Good | 24h | 2.0 ngày | 1.3 | round(24 × 2.0 × 1.3) = **62h** |
| Easy | 72h | 10 ngày | 1.5 | round(72 × 10 × 1.5) = **1080h (~45 ngày)** |
| Again | 0.5h | 3.0 ngày | 1.0 | max(1, round(0.5 × 3.0 × 1.0)) = **2h** |

---

## 8. Phân loại Mastery — `calculateMastery()`

```java
private UserProgress.MasteryLevel calculateMastery(UserProgress progress)
```

Sử dụng multi-criteria threshold:

```
NEW        → correctCount == 0
LEARNING   → correctCount < 3  OR  accuracy < 60%  OR  retrievability < 0.40
REVIEWING  → correctCount < 8  OR  accuracy < 80%  OR  retrievability < 0.75
MASTERED   → tất cả còn lại
```

**Ý nghĩa:**

| Level | Trạng thái | Hành động hệ thống |
|-------|-----------|-------------------|
| `NEW` | Chưa học lần nào | Đưa vào queue học |
| `LEARNING` | Đang học, chưa ổn định | Ôn tập thường xuyên |
| `REVIEWING` | Biết rồi, củng cố | Ôn tập theo FSRS schedule |
| `MASTERED` | Nắm vững | Interval rất dài |

---

## 9. Lấy Danh sách từ cần ôn — `getDueWords()`

```java
public List<UserProgress> getDueWords(int limit)
```

1. Query `findDueWords(user, now)` — lấy tất cả từ có `nextReview <= now`
2. Nếu chưa đủ `limit`, bổ sung từ chưa từng học (tạo `UserProgress` mới với `nextReview = now`)
3. Clamp về `limit`

---

## 10. Thống kê — `getFsrsStats()`

```java
public Map<String, Object> getFsrsStats()
```

| Metric | Nguồn | Ý nghĩa |
|--------|-------|---------|
| `dueNow` | `countDueWords(user, now)` | Số từ cần ôn ngay |
| `mastered` | `countMastered(user)` | Số từ đạt MASTERED |
| `learning` | `countLearning(user)` | Số từ đang học |
| `retentionEstimate` | Avg retrievability × 100 | Ước tính % nhớ trung bình |

---

## 11. ReviewEvent — Audit Log bất biến

```
src/main/java/com/fsrspring/vocab/model/ReviewEvent.java
```

Mỗi lần ôn tập tạo một `ReviewEvent` **không được sửa/xóa** (immutable event):

```java
ReviewEvent {
    user, word, rating, correct,
    responseTimeMs, reviewedAt
}
```

Cho phép:
- Re-simulate lại toàn bộ lịch sử nếu đổi thuật toán
- Audit trail đầy đủ
- Phân tích hành vi user sau này

---

## 12. Null-Safety Pattern

Do Lombok `@Builder.Default` chỉ áp dụng khi dùng `Builder`, không áp dụng khi JPA load từ DB, tất cả FSRS fields đều có custom getter null-safe:

```java
public Double getFsrsStability()   { return fsrsStability   != null ? fsrsStability   : 0.2; }
public Double getFsrsDifficulty()  { return fsrsDifficulty  != null ? fsrsDifficulty  : 5.0; }
public Integer getFsrsRepetition() { return fsrsRepetition  != null ? fsrsRepetition  : 0;   }
// ... v.v.
```

**Luôn dùng getter, không bao giờ truy cập trực tiếp field** khi tính toán.

---

## 13. Tóm tắt Công thức

$$\text{Difficulty}_{t} = \text{clamp}(D_{t-1} + (3 - r) \times 0.35,\ 1,\ 10)$$

$$\text{Stability}_{t} = \begin{cases}
\max(0.15,\ S_{t-1} \times 0.65) & \text{if } r = 1 \text{ (lapse)} \\
\min(365,\ S_{t-1} \times (1 + 0.12 + (r-2)\times 0.08 + A_{EMA} \times 0.15)) & \text{if } r \geq 2
\end{cases}$$

$$\text{Retrievability}_{t} = e^{-t_{hours}\ /\ \max(1,\ S_{days} \times 24)}$$

$$\text{NextInterval} = \max(1,\ \text{round}(\text{baseHours}(r) \times S_{days} \times \text{mult}_{behavior}))$$

$$\text{mult}_{behavior} = \text{clamp}\bigl(1 + A_{EMA}\times 0.4 - \text{slowPenalty} + C \times 0.2,\ 0.4,\ 2.2\bigr)$$

---

## 14. Sơ đồ Vòng đời Từ vựng

```
[NEW]
  │ user ôn lần đầu
  ▼
[LEARNING]  ←──────────────────────────────┐
  │ correctCount ≥ 3                       │
  │ accuracy ≥ 60%                         │ Again (lapse):
  │ retrievability ≥ 40%                   │ stability×0.65
  ▼                                        │ repetition=0
[REVIEWING] ←───────────── Hard/Good/Easy  │
  │ correctCount ≥ 8                       │
  │ accuracy ≥ 80%                         │
  │ retrievability ≥ 75%                   │
  ▼                                        │
[MASTERED]  ─────────────────── (Again) ───┘
  │
  ▼
interval dài dần đến 365 ngày
```

---

## 15. Files liên quan

| File | Vai trò |
|------|---------|
| [FsrsService.java](../src/main/java/com/fsrspring/vocab/service/FsrsService.java) | Core scheduling logic |
| [UserProgress.java](../src/main/java/com/fsrspring/vocab/model/UserProgress.java) | FSRS state per user/word |
| [ReviewEvent.java](../src/main/java/com/fsrspring/vocab/model/ReviewEvent.java) | Immutable review log |
| [FsrsSimulationTest.java](../src/test/java/com/fsrspring/vocab/service/FsrsSimulationTest.java) | 9 test scenarios + timeline simulation |
