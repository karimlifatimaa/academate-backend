# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Companion repository

Frontend (Next.js): `/Users/fatimakarimli/academate-frontend`

## Commands

```bash
# Run (active profile: dev by default)
./gradlew bootRun

# Compile only
./gradlew compileJava

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.academatebackend.SomeTest"

# Build jar
./gradlew build
```

**Prerequisites:** PostgreSQL running on `localhost:5432`, database `academate_dev` (user `postgres`, pass `postgres`).

Swagger UI available at `http://localhost:8080/swagger-ui.html` when running in `dev` profile.

## Architecture

**Stack:** Spring Boot 3.5 · Java 17 · PostgreSQL · JWT (jjwt) · DigitalOcean Spaces (S3) · Zoom API · Bucket4j · Springdoc OpenAPI

### Package layout

```
controller/     HTTP layer — one controller per domain
service/        Business logic — all @Transactional lives here
repository/     Spring Data JPA interfaces
entity/         JPA entities (all extend BaseEntity)
dto/            Request + Response POJOs (Lombok @Getter @Builder)
enums/          Role, Subject, LessonStatus, RelationType, …
config/         Spring beans, security, CORS, ZoomProperties, …
security/       JWT filter, SecurityConfig, CustomUserDetails
common/
  exception/    ApiException hierarchy + GlobalExceptionHandler
  validation/   Custom validators
```

### Entity model

All entities extend `BaseEntity` (UUID PK via `@UuidGenerator`, `createdAt`, `updatedAt`).

- **User** — single users table for all roles (STUDENT, TEACHER, PARENT, ADMIN). Role-specific data lives in satellite tables.
- **StudentProfile / TeacherProfile / ParentProfile** — `@OneToOne` with User, same UUID as PK.
- **TeacherSubject** — composite key `(teacherId, subject)`.
- **TeacherAvailability** — weekly recurring slots `(teacherId, dayOfWeek, startTime, endTime)`. No specific dates.
- **Lesson** — `(teacherId, studentId, subject, scheduledAt, durationMinutes, status, meetingLink, zoomMeetingId)`. Status flow: `PENDING → CONFIRMED → IN_PROGRESS → COMPLETED` (or `CANCELLED`).
- **ParentStudentLink** — composite key `(parentId, studentId)` with `RelationType` and `createdVia`.
- **StudentInviteCode** — 6-char code for parent-student linking.
- **RefreshToken** — stored as SHA-256 hash.
- **EmailVerificationToken / PasswordResetToken** — short-lived tokens.
- **AuditLog** — append-only event log for security-relevant actions.

Soft delete on User: `isActive = false`, `deletedAt` timestamp — user is never removed from DB.

### Auth flow

- Access token: 15 min JWT, sent as `Authorization: Bearer` header.
- Refresh token: 7-day, stored hashed in DB, rotated on each use.
- `JwtAuthenticationFilter` runs before every request and populates `SecurityContextHolder`.
- `RateLimitFilter` (Bucket4j) runs before JWT filter — rejects with `429` when IP exceeds limit.
- `RequestLoggingFilter` runs first — pushes a `traceId` into MDC (visible in every log line as `[traceId]`).

### Key service interactions

- `AuthService.registerTeacher()` → emails all ADMIN users via `EmailService.sendNewTeacherForVerification()`.
- `LessonService.confirm()` → calls `ZoomService.createMeeting()` (returns `ZoomMeeting(meetingId, joinUrl)`) → stores both `meetingLink` and `zoomMeetingId` on the lesson.
- `ZoomWebhookController` (public, no JWT) → verifies `x-zm-signature` → handles `meeting.started` (→ `IN_PROGRESS`) and `meeting.ended` (→ `COMPLETED`). Also handles `endpoint.url_validation` for Zoom's challenge-response.
- `AdminService.verifyTeacher()` → throws `400` if teacher profile (bio + hourlyRate) or availability is incomplete.
- S3 upload is presigned-URL based: backend generates URL, client uploads directly to DigitalOcean Spaces.

### Configuration profiles

| Profile | Activated by | Notes |
|---------|--------------|-------|
| `dev` | default | Local Postgres, real Gmail SMTP, real Zoom |
| `prod` | `SPRING_PROFILES_ACTIVE=prod` | All values from env vars |

`application-dev.yml` contains real credentials for local dev — do **not** commit new secrets there.

### Error handling

All exceptions map to RFC 7807 `ApiError` via `GlobalExceptionHandler`. Every response includes a `traceId` (from `X-Request-Id` header or generated). Response header `X-Request-Id` echoes it back for client-side correlation.

Custom exception hierarchy: `ApiException` → `BadRequestException (400)`, `UnauthorizedException (401)`, `ForbiddenException (403)`, `ResourceNotFoundException (404)`, `ConflictException (409)`.

### Booking validation

`LessonService.book()` enforces:
1. Duration must be 1–240 minutes.
2. `scheduledAt` must fall entirely within one of the teacher's weekly availability windows for that day.
3. No existing `PENDING`/`CONFIRMED` lesson for the same teacher overlaps the requested window (checked via `findOverlapCandidates` + Java-side overlap logic).

### Subjects & locale

All subject values are Azerbaijani enum constants (`RIYAZIYYAT`, `FIZIKA`, …). All user-facing error messages in services are written in Azerbaijani.
