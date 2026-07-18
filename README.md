<div align="center">

# 📚 FSRSpring Vocabulary

A vocabulary learning platform built with **Spring Boot** and **Next.js**, using the **FSRS (Free Spaced Repetition Scheduler)** algorithm to generate personalized review schedules and improve long-term vocabulary retention.

![Java](https://img.shields.io/badge/Java-17-blue?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3-6DB33F?style=for-the-badge&logo=springboot)
![Next.js](https://img.shields.io/badge/Next.js-15-black?style=for-the-badge&logo=next.js)
![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?style=for-the-badge&logo=mysql)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker)

</div>

---

## Overview

FSRSpring Vocabulary is a web application that helps users learn English vocabulary more effectively using the **Free Spaced Repetition Scheduler (FSRS)** algorithm.

Instead of reviewing words at fixed intervals, the application calculates the next review time based on each user's learning performance. This helps reduce unnecessary reviews while improving long-term memory retention.

The project also provides vocabulary management, quizzes, learning statistics, and an administration dashboard.

---

## Features

### User

- Register and login
- Learn vocabulary by topic
- Flashcard learning
- Quiz and review sessions
- Personalized review schedule using FSRS
- Track learning progress

### Administrator

- Manage users
- Manage vocabulary
- Manage topics
- View learning statistics
- Import vocabulary from external APIs

---

## Technology Stack

### Backend

- Java 17
- Spring Boot 3
- Spring Security
- Spring Data JPA
- JWT Authentication
- Maven

### Frontend

- Next.js
- React
- Tailwind CSS

### Database

- MySQL
- Redis

### Tools

- Docker
- Git
- GitHub

---

## Project Structure

```text
FSRSpring
│
├── frontend
├── src
├── docs
├── scripts
├── docker-compose.yml
└── pom.xml
```

---

## Getting Started

### Prerequisites

- Java 17
- Node.js
- Docker Desktop

### Clone the repository

```bash
git clone https://github.com/Trung5345/FSRSpring.git
cd FSRSpring
```

### Run with Docker

```bash
cp .env.example .env
docker compose up --build
```

### Run Backend

```bash
./mvnw spring-boot:run
```

### Run Frontend

```bash
cd frontend
npm install
npm run dev
```

---

## Screenshots

> Add screenshots of the application here.

Example:

- Login Page
- Dashboard
- Vocabulary List
- Flashcard
- Quiz Page

---

## APIs & Integrations

- Dictionary API
- Datamuse API
- Google OAuth2 Login
- MySQL
- Redis

---

## Testing

Run unit tests:

```bash
./mvnw clean test
```

---

## Future Improvements

- Mobile application
- More quiz types
- Better learning analytics
- Performance optimization

---

## Author

**Trung Tran**

GitHub: https://github.com/Trung5345

---

## License

This project is licensed under the MIT License.
