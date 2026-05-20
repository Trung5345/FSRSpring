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
  <li><b>Database:</b> H2 Database (Dev), scalable to PostgreSQL/MySQL via JPA</li>
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

Ensure you have the following installed on your local development environment:
- <a href="https://adoptium.net/">Java 17</a> (Strictly required for build)
- <a href="https://nodejs.org/">Node.js / Corepack</a> (Required for modern Next.js frontend)
- <a href="https://www.docker.com/products/docker-desktop">Docker Desktop</a> (Optional, for containerized deployments)
- Git version control

### 1. Backend Setup (Spring Boot)

Ensure the <code>JAVA_HOME</code> environment variable is pointing to a Java 17 JDK installation.

<b>macOS / Linux:</b>
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"
./scripts/run-local.sh
```

<b>Windows (PowerShell):</b>
```powershell
.\mvnw.cmd spring-boot:run
```
<ul>
  <li><b>API Base URL:</b> <code>http://localhost:8080</code></li>
  <li><b>H2 DB Console:</b> <code>http://localhost:8080/h2-console</code></li>
</ul>

### 2. Frontend Setup (Next.js)

The modern user interface resides within the <code>frontend/</code> directory. It requires the Spring Boot backend to be running as it handles API communication, OAuth, and data fetching locally via Next.js rewrites.

```bash
cd frontend
corepack enable
pnpm install
SPRING_API_BASE_URL=http://localhost:8080 pnpm dev
```
<ul>
  <li><b>Frontend URL:</b> <code>http://localhost:3000</code></li>
</ul>

## Deployment with Docker

You can easily spin up the entire application stack (Next.js client and Spring Boot API) using Docker Compose:

```bash
docker compose up --build
```
<ul>
  <li>Next.js Frontend: <code>http://localhost:3000</code></li>
  <li>Spring Boot API / Legacy UI: <code>http://localhost:8080</code></li>
</ul>

To stop and remove the containers, use:
```bash
docker compose down
```

## Testing & Quality Assurance

The project includes isolated unit tests for FSRS state transitions, contract tests for scheduler interfaces, and comprehensive integration tests for APIs.

<b>macOS / Linux:</b>
```bash
./mvnw clean test
```

<b>Windows:</b>
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
