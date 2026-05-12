# GitHub Copilot Instructions — FSRSpring Vocabulary

## Project Overview

FSRSpring là ứng dụng học từ vựng dựa trên thuật toán FSRS (Free Spaced Repetition Scheduler).
Stack: Java 17, Spring Boot 3, Spring Data JPA, H2 (dev), vanilla HTML/CSS/JS frontend.

## Always-Active Skills

### UI/UX — fsrspring-ui-ux skill
Khi người dùng yêu cầu bất kỳ thay đổi frontend nào (HTML, CSS, JS trong `src/main/resources/static/`),
**luôn đọc và áp dụng skill** tại:

```
/Users/nguyenhuuthang/.agents/skills/fsrspring-ui-ux/SKILL.md
```

Triggers bắt buộc dùng skill này:
- Bất kỳ chỉnh sửa file trong `src/main/resources/static/`
- Yêu cầu liên quan đến: UI, UX, giao diện, design, HTML, CSS, JavaScript frontend
- Thêm trang mới, refactor component, cải thiện accessibility, dark mode, responsive

### Architecture — CLAUDE.md
Mọi thay đổi backend phải tuân theo nguyên tắc trong `CLAUDE.md` tại root project:
- Modular monolith, inter-module qua service interface
- FSRS state transitions qua `SchedulerEngine` abstraction
- Không cross-module repository access
- SOLID principles

## Key Conventions

- **No emoji characters** trong source code — dùng SVG inline hoặc CSS icon
- **CSS custom properties** cho mọi màu sắc, spacing — không hardcode hex
- **WCAG AA** accessibility — contrast ≥ 4.5:1, keyboard navigable, ARIA labels
- **Rating phải có 4 mức**: Again / Hard / Good / Easy (không shortcut)
- **Không bypass FSRS engine** khi tính next review date

## Build & Run

```bash
./mvnw spring-boot:run          # Start app tại http://localhost:8080
./mvnw clean test               # Chạy test suite
python3 scripts/remove_emojis.py --dry-run   # Kiểm tra emoji trong source
```
