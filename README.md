# Academate Backend

REST API backend for the **Academate** platform — an online tutoring marketplace that connects students, teachers, and parents.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security + JWT (JJWT 0.12) |
| File Storage | AWS S3 |
| Email | Spring Mail (SMTP) |
| Rate Limiting | Bucket4j |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Mapping | MapStruct |
| Build | Gradle |

---

## Roles

| Role | Description |
|------|-------------|
| `STUDENT` | Books lessons, writes reviews, generates invite codes for parents |
| `TEACHER` | Manages availability, confirms/completes/cancels lessons |
| `PARENT` | Links to children via invite code, monitors activity |
| `ADMIN` | Manages users, verifies teachers, deactivates accounts |

---

## Core Features

### Authentication
- Register as Student, Teacher, or Parent
- Parent can register child accounts directly (no email/password required for young children)
- JWT-based stateless auth (access token: 15 min, refresh token: 7 days)
- Refresh token rotation with device/IP tracking
- Email verification (24h token expiry)
- Forgot password / Reset password (1h token expiry)
- Account locking after 5 failed login attempts (15 min lockout)

### Lessons
- Students book lessons with available teachers
- Teachers confirm, complete, or cancel lessons
- Lesson status flow: `PENDING → CONFIRMED → COMPLETED / CANCELLED`
- Jitsi-based meeting links generated on confirmation
- `.ics` calendar file sent via email on confirmation

### Teacher Search
- Public search by subject with pagination
- Availability slot management (day of week + time range)
- Teacher public profile with ratings and reviews

### Family Management
- Parent registers child directly (child age < 7)
- Older students generate a 6-character invite code
- Parent links via invite code with relation type (MOTHER / FATHER / GUARDIAN)

### Reviews
- Students leave reviews for teachers after lessons
- Public review listing per teacher

### User Profiles
- Avatar upload via AWS S3 (direct upload)
- Update personal info, student profile, teacher profile

### Admin Panel
- List and filter users by role
- Verify teachers
- Deactivate user accounts

---

## Planned Integrations

### AI / RAG System
- Dedicated AI microservice at `AI_SERVICE_URL` (default: `http://localhost:8000`)
- RAG (Retrieval-Augmented Generation) pipeline for intelligent tutoring assistance
- Students can ask questions — answers are generated from indexed lesson materials and course content
- Rate limited to 30 AI requests per hour per user
- 30-second timeout per AI request

---

## Project Structure

```
src/main/java/com/example/academatebackend/
├── config/          # App configuration (Google Calendar, S3, etc.)
├── controller/      # REST controllers
├── dto/             # Request & response DTOs
├── entity/          # JPA entities
├── enums/           # Role, Subject, LessonStatus, RelationType
├── repository/      # Spring Data repositories
├── security/        # JWT, filters, user details, rate limiting
├── service/         # Business logic
└── common/          # Exceptions, validators, utils
```

---

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/academate_dev` |
| `DB_USER` | Database username | `postgres` |
| `DB_PASS` | Database password | `postgres` |
| `JWT_SECRET` | JWT signing secret (min 256-bit) | `changeme-...` |
| `SMTP_HOST` | SMTP server host | `localhost` |
| `SMTP_PORT` | SMTP server port | `1025` |
| `SMTP_USER` | SMTP username | — |
| `SMTP_PASS` | SMTP password | — |
| `AWS_REGION` | AWS region | `eu-central-1` |
| `AWS_S3_BUCKET` | S3 bucket name | `academate-dev` |
| `AWS_ACCESS_KEY` | AWS access key | — |
| `AWS_SECRET_KEY` | AWS secret key | — |
| `SERVER_PORT` | HTTP server port | `8080` |

---

## Getting Started

### Prerequisites
- Java 17+
- PostgreSQL 14+
- Gradle 8+

### Run locally

```bash
# Clone the repository
git clone https://github.com/your-org/academate-backend.git
cd academate-backend

# Start PostgreSQL (example via Docker)
docker run -d \
  --name academate-db \
  -e POSTGRES_DB=academate_dev \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 postgres:16

# Run the application
./gradlew bootRun
```

The app starts on [http://localhost:8080](http://localhost:8080).

---

## API Documentation

Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON spec:

```
http://localhost:8080/v3/api-docs
```

---

## Rate Limits

| Endpoint group | Limit |
|---------------|-------|
| Login | 5 requests / minute per IP |
| Register | 3 requests / hour per IP |
| Forgot password | 3 requests / hour per IP |
| AI ask | 30 requests / hour per user |
| Content upload | 20 requests / day per user |

---

## Security Highlights

- Passwords hashed with BCrypt (12 rounds)
- Refresh & verification tokens stored as SHA-256 hashes
- One-time use tokens for email verification and password reset
- CORS restricted to allowed frontend origins
- Rate limiting via Bucket4j (token bucket algorithm)
