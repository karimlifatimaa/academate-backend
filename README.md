# Academate Backend

> REST API for the Academate platform — an online tutoring marketplace connecting students, teachers, and parents.

---

## Overview

Academate is a role-based tutoring platform where students discover and book lessons with verified teachers, parents monitor their children's progress, and admins manage the ecosystem. The backend is built with Spring Boot 3.5 and follows a stateless, JWT-secured REST architecture.

---

## Tech Stack

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)

| Concern | Choice |
|---------|--------|
| Framework | Spring Boot 3.5 |
| Security | Spring Security + JWT (JJWT 0.12) |
| Database | PostgreSQL / Spring Data JPA |
| File Storage | AWS S3 |
| Email | Spring Mail |
| Rate Limiting | Bucket4j |
| API Docs | SpringDoc OpenAPI |
| Build | Gradle 8 |

---

## Features

### Auth & Identity
- Multi-role registration: Student, Teacher, Parent
- Parents can create child accounts without email or password
- JWT access + refresh token pair with rotation
- Email verification and password reset via one-time tokens
- Account lockout after repeated failed logins

### Lessons
- Students book lessons with teachers by subject and time slot
- Status lifecycle: `PENDING → CONFIRMED → COMPLETED / CANCELLED`
- Jitsi meeting link generated on confirmation
- Calendar invite (`.ics`) delivered by email

### Teacher Discovery
- Public search and filter by subject
- Weekly availability schedule per teacher
- Rating aggregation from student reviews

### Family Management
- Parents link to existing student accounts via 6-character invite codes
- Relation types: Mother, Father, Guardian
- Parents register young children directly under their account

### Reviews
- Students rate and review teachers post-lesson
- Public review feed per teacher profile

### Admin
- User listing and role filtering
- Teacher verification workflow
- Account deactivation

### Planned — AI / RAG Integration
- Dedicated AI microservice connected via internal HTTP
- RAG (Retrieval-Augmented Generation) pipeline over lesson materials
- Students ask questions; answers are grounded in indexed course content

---

## Project Structure

```
src/main/java/com/example/academatebackend/
├── config/        configuration beans
├── controller/    REST endpoints
├── dto/           request & response objects
├── entity/        JPA entities
├── enums/         Role, Subject, LessonStatus, RelationType
├── repository/    Spring Data repositories
├── security/      JWT filter, user details, rate limiter
├── service/       business logic
└── common/        exceptions, validators, utilities
```

---

## API Documentation

Interactive API docs are served by Swagger UI at `/swagger-ui.html` when the application is running.

---

## License

MIT
