# FSRSpring Vocabulary

Spring Boot application for vocabulary learning with quiz and progress tracking.

## Requirements

- Java 17
- Docker Desktop (optional, for container run)
- Git

Maven is bundled via Maven Wrapper, so no global Maven install is required.

## Java 17 Setup (Important)

This project enforces Java 17 at build time.

macOS (zsh):

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

Windows (PowerShell):

```powershell
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17*"
$env:JAVA_HOME = (Get-ChildItem 'C:\Program Files\Eclipse Adoptium\' -Directory | Where-Object { $_.Name -like 'jdk-17*' } | Select-Object -First 1).FullName
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
```

If `java -version` is not 17.x, update JAVA_HOME before running Maven commands.

## Run Locally (macOS/Linux)

```bash
./mvnw spring-boot:run
```

## Run Locally (Windows PowerShell)

```powershell
.\mvnw.cmd spring-boot:run
```

Application URL: http://localhost:8080

H2 Console URL: http://localhost:8080/h2-console

## Build And Test

macOS/Linux:

```bash
./mvnw clean test
```

Windows PowerShell:

```powershell
.\mvnw.cmd clean test
```

## Run With Docker

Build and run with Docker Compose:

```bash
docker compose up --build
```

Stop containers:

```bash
docker compose down
```

If your Docker setup still uses the legacy command, replace `docker compose` with `docker-compose`.

## CI/CD (GitHub Actions)

Workflow file: `.github/workflows/ci-cd.yml`

- CI:
	- Triggered on pull requests to `main` and pushes to `main`.
	- Runs matrix tests on `ubuntu-latest`, `macos-latest`, and `windows-latest` with Java 17.
- CD:
	- On push to `main`, after tests pass, builds and publishes Docker image to GHCR.
	- Image tags: `latest` and commit `sha`.

Published image name format:

```text
ghcr.io/<owner>/<repo>:latest
ghcr.io/<owner>/<repo>:sha-<commit>
```

## Project Structure

```text
src/main/java/com/fsrspring/vocab
src/main/resources
src/test/java/com/fsrspring/vocab
```