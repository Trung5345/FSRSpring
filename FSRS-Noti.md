# Gợi ý thời điểm thông báo khi dùng thuật toán FSRS

Với **FSRS + notification**, hợp lý nhất là: **chỉ thông báo khi có thẻ đã đến hạn review, hoặc sắp đến giờ học cố định của người dùng mà hôm đó có thẻ đến hạn**. Không nên thông báo mỗi khi thuật toán tính ra một mốc riêng cho từng thẻ, vì sẽ rất phiền.

FSRS bản chất là scheduler dựa trên trạng thái trí nhớ như **difficulty, stability, retrievability**, và mục tiêu là cho review khi xác suất nhớ giảm về mức retention mong muốn. Vì vậy notification nên bám vào **due date / due queue**, không bám vào từng thẻ riêng lẻ.

## 1. Thông báo chính: vào khung giờ học cố định

Ví dụ:

> Bạn có 24 thẻ cần ôn hôm nay.

Nên cho user chọn giờ, ví dụ **8:00 sáng**, **20:00 tối**, hoặc “sau giờ học/làm”. Đây là thông báo quan trọng nhất.

Logic:

```text
Mỗi ngày vào giờ user chọn:
  nếu due_cards_count > 0:
      gửi thông báo
  nếu due_cards_count = 0:
      không gửi
```

Đây là cách sạch nhất, ít gây khó chịu nhất.

## 2. Thông báo nhắc lại nếu sắp hết ngày mà chưa học

Ví dụ lúc **20:30 hoặc 21:00**:

> Bạn còn 12 thẻ chưa ôn hôm nay.

Chỉ nên gửi nếu:

```text
due_cards_count > 0
và user chưa review hôm nay
hoặc còn nhiều thẻ due chưa làm
```

Không nên gửi quá muộn, ví dụ 23:30, vì dễ phản tác dụng.

## 3. Với thẻ quá hạn: gom lại, không spam

Nếu user bỏ lỡ 2–3 ngày, chỉ gửi kiểu:

> Bạn có 63 thẻ đang quá hạn. Ôn 10 phút để giữ streak nhé.

Không nên mỗi ngày gửi nhiều lần kiểu “bạn đang quên thẻ này”, vì FSRS đã chấp nhận việc review có thể sớm hoặc trễ và vẫn tự thích nghi theo lịch sử học của người dùng.

## 4. Không nên thông báo ngay khi từng card đến hạn

Không nên làm kiểu:

```text
10:03 - Card A đến hạn
10:08 - Card B đến hạn
10:15 - Card C đến hạn
```

Cách này rất tệ với UX. Flashcard nên review theo **session**, không phải theo từng card riêng lẻ.

## Rule đề xuất cho app

Nên dùng 3 loại notification:

| Loại                      | Khi nào gửi                          | Nội dung                                   |
| ------------------------- | ------------------------------------ | ------------------------------------------ |
| Daily due reminder        | Giờ user chọn, ví dụ 8:00 hoặc 20:00 | Bạn có X thẻ cần ôn hôm nay                |
| Evening fallback          | 20:30–21:30 nếu chưa học             | Bạn còn X thẻ chưa ôn                      |
| Streak / overdue reminder | Sau 1–2 ngày không học               | Bạn có X thẻ quá hạn, ôn nhanh 10 phút nhé |

## Công thức logic đơn giản

```js
if (now === userPreferredReminderTime) {
  if (dueCardsToday > 0) {
    sendNotification(`Bạn có ${dueCardsToday} thẻ cần ôn hôm nay.`);
  }
}

if (now === eveningReminderTime) {
  if (remainingDueCards > 0 && !hasStudiedToday) {
    sendNotification(`Bạn còn ${remainingDueCards} thẻ chưa ôn hôm nay.`);
  }
}

if (daysInactive >= 2 && overdueCards > 0) {
  sendNotification(`Bạn có ${overdueCards} thẻ quá hạn. Ôn 10 phút để giữ nhịp nhé.`);
}
```

## Kết luận thực tế

Với web/app học flashcard, nên làm như sau:

**Mặc định gửi 1 thông báo/ngày vào 20:00 nếu có thẻ due.**
Nếu user chưa học, gửi thêm **1 nhắc nhẹ vào 21:30**.
Nếu user bỏ học nhiều ngày, gửi **1 thông báo comeback mỗi 2–3 ngày**, không gửi hằng giờ.

Mức này vừa đúng tinh thần FSRS, vừa giữ retention tốt, lại không làm user thấy app phiền.
