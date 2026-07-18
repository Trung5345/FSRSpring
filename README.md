<div align="center">
  <h1>FSRSpring Vocabulary</h1>
  <p><strong>A smart, automated vocabulary learning system powered by the Free Spaced Repetition Scheduler (FSRS) and Spring Boot.</strong></p>
  
  <div>
    <img src="https://img.shields.io/badge/Java-17-blue.svg?style=for-the-badge&logo=java" alt="Java 17" />
    <img src="https://img.shields.io/badge/Spring_Boot-3-brightgreen.svg?style=for-the-badge&logo=spring" alt="Spring Boot" />
    <img src="https://img.shields.io/badge/Algorithm-FSRS-orange.svg?style=for-the-badge" alt="FSRS Algorithm" />
    <img src="https://img.shields.io/badge/Frontend-Next.js%20%7C%20Tailwind-black.svg?style=for-the-badge&logo=next.js" alt="Frontend" />
    <img src="https://img.shields.io/badge/Build-Maven-C71A22.svg?style=for-the-badge&logo=apache-maven" alt="Maven" />
  </div>
</div>

<br />

> FSRSpring Vocabulary is a modern, modular web application designed to help users master vocabulary efficiently. By integrating the state-of-the-art **FSRS (Free Spaced Repetition Scheduler)** algorithm, this project dynamically calculates optimal review times to minimize daily workload while maximizing long-term retention.

<br />

## Key Features

<table>
  <tr>
    <td width="50%">
      <h3>FSRS-Driven Scheduling</h3>
      <p>Precise, algorithmic spaced repetition based on active memory states, adapting automatically to user performance rather than relying on static intervals.</p>
    </td>
    <td width="50%">
      <h3>Automated Word Enrichment</h3>
      <p>Built-in pipeline leveraging Datamuse and Dictionary APIs to fetch and enrich word definitions, pronunciations, and practical usage examples.</p>
    </td>
  </tr>
  <tr>
    <td width="50%">
      <h3>Comprehensive Analytics</h3>
      <p>Data-driven tracking of learning progress, mastery levels, review logs, and daily streaks to provide complete insight into retention metrics.</p>
    </td>
    <td width="50%">
      <h3>Modular Monolith Architecture</h3>
      <p>Strictly bounded contexts with clear inter-module communication, isolating domain logic from infrastructure to ensure high maintainability.</p>
    </td>
  </tr>
</table>

## Architecture & Technology Stack

This project adheres to a **Modular Monolith** architectural style. Inter-module communication flows through explicit public service interfaces to keep domains cleanly separated.

### Core Stack
<ul>
  <li><b>Backend:</b> Java 17, Spring Boot 3, Spring Data JPA, Spring Security</li>
  <li><b>Database:</b> MySQL 8.4, Redis 7 (session store)</li>
  <li><b>Frontend (Modern):</b> Next.js 15, React, Tailwind CSS</li>
  <li><b>Frontend (Legacy):</b> Vanilla HTML/CSS/JavaScript with Tailwind CSS</li>
</ul>

### Domain Modules
<ul>
  <li><code>word-management</code>: Word CRUD operations, metadata, API enrichment, imports.</li>
  <li><code>review-scheduling</code>: FSRS state transitions, next review date calculations.</li>
  <li><code>learning-session</code>: Quiz workflows, flashcard learning orchestration.</li>
  <li><code>progress-analytics</code>: Mastery analytics, user streaks, and retention metrics.</li>
</ul>

## Getting Started

### Prerequisites

- <a href="https://adoptium.net/">Java 17</a>
- <a href="https://www.docker.com/products/docker-desktop">Docker Desktop</a> (required — MySQL, Redis, LibreTranslate run in containers)
- <a href="https://nodejs.org/">Node.js / Corepack</a> (required for Next.js frontend only)

### Option A — Docker Compose (recommended)

Spins up the full stack: Spring Boot API, Next.js frontend, MySQL, Redis, and LibreTranslate.

```bash
cp .env.example .env   # edit credentials if needed
docker compose up --build
```

| Service | URL |
|---|---|
| Legacy UI / API | http://localhost:8080 |
| Next.js Frontend | http://localhost:3000 |

```bash
docker compose down        # stop containers
docker compose down -v     # stop and delete volumes (wipes database)
```

### Option B — Run backend locally (dev mode)

Use this when you want fast iteration on the backend without rebuilding Docker images. The infrastructure services (MySQL, Redis) still run in Docker.

**Step 1 — Start infrastructure:**
```bash
docker compose up -d mysql redis libretranslate
```

**Step 2 — Copy and configure environment:**
```bash
cp .env.example .env   # edit credentials if needed
```

**Step 3 — Run Spring Boot with the `local` profile:**

macOS / Linux (recommended — auto-frees port 8080 if occupied):
```bash
./scripts/dev.sh
```

Or manually:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Windows (PowerShell):
```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

The `local` profile (`src/main/resources/application-local.properties`) connects to:
- MySQL on `localhost:3307`
- Redis on `localhost:6380`

> `scripts/dev.sh` also starts infrastructure services automatically and stops any Docker container or process occupying port 8080 before launching.

**Step 4 — Start the frontend (optional):**
```bash
cd frontend
corepack enable
pnpm install
SPRING_API_BASE_URL=http://localhost:8080 pnpm dev
```

| Service | URL |
|---|---|
| Spring Boot API / Legacy UI | http://localhost:8080 |
| Next.js Frontend | http://localhost:3000 |

### Environment variables

Copy `.env.example` to `.env` and fill in the required values:

| Variable | Description | Default |
|---|---|---|
| `MYSQL_USER` / `MYSQL_PASSWORD` | MySQL credentials | `fsr_user` / `fsr_pass` |
| `REDISPASSWORD` | Redis password | `fsr_redis_pass_123` |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | OAuth2 Google login | — |
| `JWT_SECRET` | 32+ byte secret to enable local JWT login | — |
| `YOUTUBE_API_KEY` / `NEWS_API_KEY` | External content APIs | — |

## Testing & Quality Assurance

The project includes isolated unit tests for FSRS state transitions, contract tests for scheduler interfaces, and comprehensive integration tests for APIs.

macOS / Linux:
```bash
./mvnw clean test
```

Windows:
```powershell
.\mvnw.cmd clean test
```

## Development Guidelines

To ensure the project remains scalable, performant, and robust, adhere to the following principles when contributing:

1. **Strict FSRS Scheduling:** Never bypass the FSRS engine. All scheduling decisions and review dates must be calculated via core algorithmic variables.
2. **Architectural Boundaries:** Controllers must never call repositories directly. Domain logic lives exclusively in domain classes and service layers. Zero cross-module repository leakage is permitted.
3. **UI/Accessibility Standards:** Ensure strict WCAG AA compliance (minimum contrast ratio 4.5:1), complete keyboard navigability, and maintain clean separation of CSS Custom Properties for theme support.
4. **Design Documentation:** Review the internal [`CLAUDE.md`](./CLAUDE.md) specification for deeper insights into the project's Modular Monolith direction and algorithmic optimizations.

## Continuous Integration / Continuous Deployment

A fully automated CI/CD pipeline is available at <code>.github/workflows/ci-cd.yml</code>:
<ul>
  <li><b>Continuous Integration:</b> Triggers on pull requests and pushes to the <code>main</code> branch. Executes comprehensive matrix tests across <code>ubuntu-latest</code>, <code>macos-latest</code>, and <code>windows-latest</code> running Java 17.</li>
  <li><b>Continuous Deployment:</b> On a successful merge or push to <code>main</code>, the workflow builds and distributes the respective Docker image to the GitHub Container Registry (<code>ghcr.io</code>).</li>
</ul>

<br />

<div align="center">
  <p>Released under the <a href="LICENSE">MIT License</a>.</p>
</div>
## Project maintained by Trung5345