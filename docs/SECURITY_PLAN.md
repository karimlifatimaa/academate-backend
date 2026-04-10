# Academate Backend — Security & İlkin Quruluş Planı

> **Status:** Təsdiqlənmiş qərarlar əsasında MVP planı
> **Build tool:** Gradle
> **Migration:** MVP üçün Flyway istifadə edilmir (post-MVP-də əlavə olunacaq)

---

## 1. Təsdiqlənmiş Qərarlar

| # | Mövzu | Qərar |
|---|-------|-------|
| 1 | DB | PostgreSQL |
| 2 | Storage | AWS S3 |
| 3 | Frontend | Web + Mobile (gələcəkdə) |
| 4 | Java | Java 17 |
| 5 | Spring Boot | 3.5.13 |
| 6 | Auth | JWT (access 15dəq + refresh 7gün) |
| 7 | Password hashing | BCrypt (strength 12) |
| 8 | Authorization | Role-based (`@PreAuthorize`) |
| 9 | Migration tool | MVP-də YOX (JPA `ddl-auto=update` — Hibernate entity-lərdən cədvəl yaradır) |
| 10 | Rate limiting | Bucket4j (in-memory MVP üçün, sonra Redis) |

### Şagird Qeydiyyat Flow-u (Hibrid — yaşa görə)

**Minimum yaş: 5**

**5–12 yaş (CHILD):**
- Şagird özü qeydiyyatdan keçə **bilməz**.
- Valideyn əvvəlcə öz hesabını yaradır → "Uşaq əlavə et" → şagird profili yaradılır.
- Email tələb olunmur. Sistem avtomatik **unikal username + random şifrə** generate edir.
- Valideyn bu credentials-i uşağına verir.
- `parent_student_links.verified = true` (çünki valideyn özü yaradıb).

**13+ yaş (TEEN):**
- Şagird özü email ilə qeydiyyatdan keçir.
- Profilindən 6 rəqəmli **invite code** generate edir (24 saat keçərli, bir dəfəlik).
- Valideyn öz hesabından bu kodla uşağına bağlanır.
- Şagird bildirişi təsdiqləyəndən sonra `verified = true`.

**Yaş `student_profiles.birth_date`-dən hesablanır** (ayrıca `age_group` field-i lazım deyil).

---

## 2. Data Model (MVP) — Final Sxem

### 2.1 `users` (core auth)
```
id                      UUID PK
email                   VARCHAR(255) UNIQUE         -- nullable (5-12 yaş şagirdlər üçün)
username                VARCHAR(50)  UNIQUE         -- nullable (yalnız uşaq şagirdlər üçün)
password_hash           VARCHAR(255) NOT NULL
full_name               VARCHAR(255) NOT NULL
role                    ENUM(STUDENT, TEACHER, PARENT, ADMIN) NOT NULL
phone                   VARCHAR(20)
avatar_url              VARCHAR(500)
preferred_language      VARCHAR(5) DEFAULT 'az'
is_active               BOOLEAN DEFAULT true
email_verified_at       TIMESTAMPTZ
last_login_at           TIMESTAMPTZ
failed_login_attempts   SMALLINT DEFAULT 0
locked_until            TIMESTAMPTZ
deleted_at              TIMESTAMPTZ       -- soft delete
created_at              TIMESTAMPTZ NOT NULL
updated_at              TIMESTAMPTZ NOT NULL
CONSTRAINT chk_email_or_username CHECK (email IS NOT NULL OR username IS NOT NULL)
```
> Login həm email, həm username ilə mümkündür (`CustomUserDetailsService.loadUserByUsername()` hər ikisini yoxlayır).

### 2.2 `student_profiles`
```
user_id      UUID PK → users.id
grade        SMALLINT CHECK (grade BETWEEN 1 AND 11)
school_name  VARCHAR(255)
city         VARCHAR(100)
birth_date   DATE                -- yaş yoxlaması üçün
```
> `parent_id` buradan silindi — ayrıca `parent_student_links` cədvəlinə köçdü (N:M).

### 2.3 `teacher_profiles`
```
user_id      UUID PK → users.id
bio          TEXT
hourly_rate  DECIMAL(8,2)
rating       DECIMAL(2,1) DEFAULT 0.0
is_verified  BOOLEAN DEFAULT false    -- admin təsdiqi
verified_at  TIMESTAMPTZ
verified_by  UUID → users.id
```

### 2.4 `parent_profiles`
```
user_id       UUID PK → users.id
occupation    VARCHAR(100)
```
> `phone` `users`-ə köçdü.

### 2.5 `parent_student_links` (YENİ)
```
parent_id   UUID → users.id
student_id  UUID → users.id
relation    ENUM(MOTHER, FATHER, GUARDIAN)
verified    BOOLEAN DEFAULT false      -- valideyn özü yaradıbsa true, invite code flow-da şagird təsdiqindən sonra true
created_via ENUM(PARENT_CREATED, INVITE_CODE) NOT NULL
linked_at   TIMESTAMPTZ NOT NULL
PK: (parent_id, student_id)
```

**`student_invite_codes`** (YENİ — 13+ yaş flow üçün)
```
id          UUID PK
student_id  UUID → users.id
code        VARCHAR(6) UNIQUE NOT NULL   -- 6 rəqəm
expires_at  TIMESTAMPTZ NOT NULL          -- 24 saat
used_at     TIMESTAMPTZ
used_by     UUID → users.id               -- kod istifadə edən valideyn
created_at  TIMESTAMPTZ NOT NULL
```

### 2.6 `teacher_subjects`
```
teacher_id  UUID → users.id
subject     ENUM(RIYAZIYYAT, FIZIKA, KIMYA, BIOLOGIYA, INFORMATIKA, AZERBAYCAN_DILI, EDEBIYYAT, INGILIS_DILI, TARIX, COGRAFIYA)
grade_min   SMALLINT
grade_max   SMALLINT
PK: (teacher_id, subject)
```

### 2.7 `content_materials`
```
id              UUID PK
teacher_id      UUID → users.id
title           VARCHAR(500) NOT NULL
description     TEXT
subject         ENUM NOT NULL
grade           SMALLINT NOT NULL
topic           VARCHAR(255)
content_type    ENUM(VIDEO, PDF, PPTX, DOCUMENT, QUIZ) NOT NULL
file_url        VARCHAR(1000) NOT NULL    -- S3 URL
file_size_bytes BIGINT
thumbnail_url   VARCHAR(500)
duration_sec    INTEGER
is_free         BOOLEAN DEFAULT true
status          ENUM(DRAFT, PUBLISHED, ARCHIVED, FLAGGED) DEFAULT 'DRAFT'
view_count      INTEGER DEFAULT 0
created_at      TIMESTAMPTZ NOT NULL
updated_at      TIMESTAMPTZ NOT NULL
```

### 2.8 `ai_sessions`
```
id          UUID PK
user_id     UUID → users.id
subject     ENUM
tokens_used INTEGER DEFAULT 0
created_at  TIMESTAMPTZ NOT NULL
```

### 2.9 `ai_messages`
```
id          UUID PK
session_id  UUID → ai_sessions.id
role        ENUM(USER, ASSISTANT) NOT NULL
content     TEXT NOT NULL
image_url   VARCHAR(500)
intent      VARCHAR(50)
latency_ms  INTEGER
tokens_used INTEGER
feedback    SMALLINT CHECK (feedback IN (-1, 0, 1))
created_at  TIMESTAMPTZ NOT NULL
```

### 2.10 `student_progress`
```
id               UUID PK
student_id       UUID → users.id
subject          ENUM NOT NULL
topic            VARCHAR(255) NOT NULL
mastery_score    DECIMAL(3,2) DEFAULT 0.00
questions_asked  INTEGER DEFAULT 0
correct_answers  INTEGER DEFAULT 0
last_studied_at  TIMESTAMPTZ
UNIQUE: (student_id, subject, topic)
```

### 2.11 Security Cədvəlləri

**`refresh_tokens`**
```
id           UUID PK
user_id      UUID → users.id
token_hash   VARCHAR(255) NOT NULL     -- SHA-256 hash, plain saxlanmır
device_info  VARCHAR(255)              -- User-Agent
ip_address   VARCHAR(45)
expires_at   TIMESTAMPTZ NOT NULL
revoked_at   TIMESTAMPTZ
created_at   TIMESTAMPTZ NOT NULL
INDEX: (user_id), (token_hash), (expires_at)
```

**`email_verification_tokens`**
```
id          UUID PK
user_id     UUID → users.id
token_hash  VARCHAR(255) NOT NULL
expires_at  TIMESTAMPTZ NOT NULL      -- 24 saat
used_at     TIMESTAMPTZ
created_at  TIMESTAMPTZ NOT NULL
```

**`password_reset_tokens`**
```
id          UUID PK
user_id     UUID → users.id
token_hash  VARCHAR(255) NOT NULL
expires_at  TIMESTAMPTZ NOT NULL      -- 1 saat
used_at     TIMESTAMPTZ
created_at  TIMESTAMPTZ NOT NULL
```

**`audit_logs`**
```
id           UUID PK
user_id      UUID → users.id (nullable)
action       VARCHAR(100) NOT NULL    -- LOGIN_SUCCESS, LOGIN_FAILED, PASSWORD_RESET, ROLE_CHANGED...
entity_type  VARCHAR(50)
entity_id    UUID
ip_address   VARCHAR(45)
user_agent   VARCHAR(500)
metadata     JSONB
created_at   TIMESTAMPTZ NOT NULL
INDEX: (user_id, created_at), (action, created_at)
```

### 2.12 Əlaqələr Xülasəsi
```
users 1──1 student_profiles
users 1──1 teacher_profiles
users 1──1 parent_profiles
users N──M users  via parent_student_links
users 1──N teacher_subjects
users 1──N content_materials   (teacher_id)
users 1──N ai_sessions
ai_sessions 1──N ai_messages
users 1──N student_progress
users 1──N refresh_tokens
users 1──N audit_logs
```

---

## 3. Texnologiya Stack

| Layer | Texnologiya |
|-------|-------------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.13 |
| Build | Gradle |
| DB | PostgreSQL 16 |
| ORM | Spring Data JPA + Hibernate |
| Security | Spring Security 6 + JJWT 0.12 |
| Validation | Jakarta Validation (Hibernate Validator) |
| Mapping | MapStruct |
| Boilerplate | Lombok |
| Rate Limit | Bucket4j |
| Storage | AWS S3 (SDK v2) |
| Email | Spring Mail (SMTP) |
| Test | JUnit 5, Mockito, Testcontainers |
| API Docs | springdoc-openapi (Swagger UI) |

### Gradle Dependencies (əsas)
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-mail'

    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    runtimeOnly    'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly    'io.jsonwebtoken:jjwt-jackson:0.12.6'

    implementation 'com.bucket4j:bucket4j-core:8.10.1'
    implementation 'software.amazon.awssdk:s3:2.28.0'
    implementation 'org.mapstruct:mapstruct:1.6.2'
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.6.2'

    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0'

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    runtimeOnly 'org.postgresql:postgresql'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'org.testcontainers:postgresql'
    testImplementation 'org.testcontainers:junit-jupiter'
}
```

---

## 4. Qovluq Strukturu

```
src/main/java/com/example/academatebackend/
├── AcademateBackendApplication.java
├── config/
│   ├── SecurityConfig.java
│   ├── CorsConfig.java
│   ├── JacksonConfig.java
│   ├── S3Config.java
│   ├── OpenApiConfig.java
│   └── properties/
│       ├── JwtProperties.java
│       └── S3Properties.java
├── common/
│   ├── entity/BaseEntity.java
│   ├── exception/
│   │   ├── ApiException.java
│   │   ├── ResourceNotFoundException.java
│   │   ├── UnauthorizedException.java
│   │   ├── ForbiddenException.java
│   │   ├── ConflictException.java
│   │   └── GlobalExceptionHandler.java
│   ├── dto/ApiError.java
│   ├── audit/
│   │   ├── AuditLog.java
│   │   ├── AuditLogRepository.java
│   │   ├── AuditLogService.java
│   │   └── AuditAction.java (enum)
│   └── ratelimit/
│       ├── RateLimitFilter.java
│       └── RateLimitService.java
├── security/
│   ├── jwt/
│   │   ├── JwtService.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── JwtAuthenticationEntryPoint.java
│   ├── CustomUserDetails.java
│   ├── CustomUserDetailsService.java
│   └── SecurityUtils.java
├── auth/
│   ├── controller/AuthController.java
│   ├── service/AuthService.java
│   ├── service/EmailVerificationService.java
│   ├── service/PasswordResetService.java
│   ├── service/RefreshTokenService.java
│   ├── dto/
│   │   ├── request/
│   │   │   ├── RegisterStudentRequest.java
│   │   │   ├── RegisterTeacherRequest.java
│   │   │   ├── RegisterParentRequest.java
│   │   │   ├── LoginRequest.java
│   │   │   ├── RefreshTokenRequest.java
│   │   │   ├── ForgotPasswordRequest.java
│   │   │   └── ResetPasswordRequest.java
│   │   └── response/
│   │       ├── AuthResponse.java
│   │       └── UserResponse.java
│   └── entity/
│       ├── RefreshToken.java
│       ├── EmailVerificationToken.java
│       └── PasswordResetToken.java
├── user/
│   ├── entity/User.java
│   ├── entity/Role.java (enum)
│   ├── repository/UserRepository.java
│   └── service/UserService.java
├── student/
│   ├── entity/StudentProfile.java
│   ├── repository/StudentProfileRepository.java
│   └── service/StudentService.java
├── teacher/
│   ├── entity/TeacherProfile.java
│   ├── entity/TeacherSubject.java
│   ├── entity/Subject.java (enum)
│   ├── repository/TeacherProfileRepository.java
│   └── service/TeacherService.java
├── parent/
│   ├── entity/ParentProfile.java
│   ├── entity/ParentStudentLink.java
│   ├── repository/ParentStudentLinkRepository.java
│   └── service/ParentService.java
├── content/
│   ├── entity/ContentMaterial.java
│   ├── entity/ContentType.java (enum)
│   ├── entity/ContentStatus.java (enum)
│   ├── repository/ContentMaterialRepository.java
│   ├── service/ContentService.java
│   ├── service/S3StorageService.java
│   └── controller/ContentController.java
├── ai/
│   ├── entity/AiSession.java
│   ├── entity/AiMessage.java
│   ├── repository/AiSessionRepository.java
│   ├── repository/AiMessageRepository.java
│   ├── service/AiService.java           -- Python RAG service client
│   └── controller/AiController.java
└── progress/
    ├── entity/StudentProgress.java
    ├── repository/StudentProgressRepository.java
    └── service/ProgressService.java

src/main/resources/
├── application.yml
├── application-dev.yml
└── application-prod.yml
```
> MVP-də `ddl-auto=update` ilə Hibernate cədvəlləri entity-lərdən avtomatik yaradır. Manual SQL schema saxlanmır.

---

## 5. Security Arxitekturası

### 5.1 JWT Strategiyası
- **Access Token:** 15 dəqiqə, stateless, HMAC-SHA256, claim-lər: `sub` (userId), `email`, `role`, `iat`, `exp`, `jti`.
- **Refresh Token:** 7 gün, DB-də SHA-256 hash olaraq saxlanır, device-info ilə bağlı, istifadədən sonra rotate olunur (refresh token rotation).
- **Secret:** `JWT_SECRET` env variable, minimum 256 bit.
- **Signature:** HS256 (MVP), RS256-ya köçmə opsiyası gələcək üçün.

### 5.2 Filter Chain
```
Request
  → CorsFilter
  → RateLimitFilter          (login, register, ai endpoint-lərə)
  → JwtAuthenticationFilter  (Bearer token varsa parse edib SecurityContext-ə qoy)
  → UsernamePasswordAuthenticationFilter
  → Authorization (@PreAuthorize)
  → Controller
```

### 5.3 Endpoint-lərin Qorunması
| Endpoint | Auth | Rol |
|----------|------|-----|
| `POST /api/v1/auth/**` | Public | — |
| `GET /api/v1/content/public/**` | Public | — |
| `GET /api/v1/ai/**` | Auth | STUDENT |
| `POST /api/v1/content/**` | Auth | TEACHER |
| `GET /api/v1/progress/me` | Auth | STUDENT |
| `GET /api/v1/progress/child/{id}` | Auth | PARENT (linked + verified) |
| `POST /api/v1/admin/**` | Auth | ADMIN |

### 5.4 Password Policy
- Min 8 simvol
- Ən azı 1 böyük, 1 kiçik hərf, 1 rəqəm
- Custom validator: `@StrongPassword`
- BCrypt strength: 12

### 5.5 Brute-force Qoruması
- 5 uğursuz login → hesab 15 dəqiqəlik kilidlənir (`locked_until`)
- Hər uğursuz cəhd `failed_login_attempts` artırılır
- Uğurlu login sıfırlayır
- `audit_logs`-a yazılır

### 5.6 Rate Limiting (Bucket4j)
| Endpoint | Limit |
|----------|-------|
| `POST /auth/login` | 5 / dəq / IP |
| `POST /auth/register/**` | 3 / saat / IP |
| `POST /auth/forgot-password` | 3 / saat / IP |
| `POST /ai/ask` | 30 / saat / user |
| `POST /content/upload` | 20 / gün / user |

### 5.7 S3 Upload Security
- Pre-signed URL pattern (client birbaşa S3-ə yükləyir, backend yalnız URL imzalayır)
- File type whitelist: PDF, MP4, PPTX, DOCX, PNG, JPG
- Size limit: PDF 50MB, Video 500MB, Image 10MB
- File key format: `{env}/{content_type}/{teacher_id}/{uuid}-{sanitized_name}`
- Server-side encryption: SSE-S3 (AES-256)

### 5.8 CORS
- Allowed origins: env variable (dev: `localhost:3000,localhost:5173`, prod: real domain)
- Allowed methods: `GET, POST, PUT, DELETE, PATCH, OPTIONS`
- Allowed headers: `Authorization, Content-Type, X-Request-Id`
- `allowCredentials: true`

### 5.9 Audit Logging
Bu event-lər `audit_logs` cədvəlinə yazılır:
- `LOGIN_SUCCESS`, `LOGIN_FAILED`, `LOGOUT`
- `REGISTER`, `EMAIL_VERIFIED`
- `PASSWORD_RESET_REQUESTED`, `PASSWORD_RESET_COMPLETED`, `PASSWORD_CHANGED`
- `ACCOUNT_LOCKED`, `ACCOUNT_UNLOCKED`
- `ROLE_CHANGED`, `USER_DEACTIVATED`
- `TEACHER_VERIFIED`
- `CONTENT_DELETED`
- `PARENT_LINKED`, `PARENT_LINK_VERIFIED`

Tətbiq: `@EventListener` + async publisher (Spring `ApplicationEventPublisher`).

### 5.10 Global Exception Handling
RFC 7807 Problem Details formatı:
```json
{
  "type": "https://academate.az/errors/validation",
  "title": "Validation Failed",
  "status": 400,
  "detail": "email: must be a valid email",
  "traceId": "abc-123",
  "timestamp": "2026-04-10T10:00:00Z"
}
```

---

## 6. Auth Endpoint-ləri (MVP)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/auth/register/student` | Şagird qeydiyyatı (email, password, grade, school, birthDate) |
| POST | `/api/v1/auth/register/teacher` | Müəllim qeydiyyatı (email, password, subjects) |
| POST | `/api/v1/auth/register/parent` | Valideyn qeydiyyatı (email, password, phone) |
| POST | `/api/v1/auth/login` | Login (email, password) → access + refresh |
| POST | `/api/v1/auth/refresh` | Refresh token → yeni access + refresh (rotation) |
| POST | `/api/v1/auth/logout` | Refresh token revoke |
| POST | `/api/v1/auth/logout-all` | Bütün device-lərdən çıxış |
| GET  | `/api/v1/auth/verify-email?token=` | Email təsdiqi |
| POST | `/api/v1/auth/resend-verification` | Verification email-i yenidən göndər |
| POST | `/api/v1/auth/forgot-password` | Şifrə bərpa emaili göndər |
| POST | `/api/v1/auth/reset-password` | Token ilə şifrə sıfırla |
| POST | `/api/v1/auth/change-password` | Auth ilə şifrə dəyiş (köhnə + yeni) |
| GET  | `/api/v1/auth/me` | Cari istifadəçi məlumatı |

---

## 7. Tətbiq Sırası (Step-by-Step)

### Addım 1 — Layihə Konfiqurasiyası
- [x] `build.gradle`-ə dependencies əlavə et
- [x] `application.yml` + `application-dev.yml` + `application-prod.yml` yarat
- [x] Env variable-lar: `DB_URL`, `DB_USER`, `DB_PASS`, `JWT_SECRET`, `AWS_*`, `SMTP_*`
- [x] `spring.jpa.hibernate.ddl-auto=update` (MVP — Hibernate cədvəlləri avtomatik yaradır)
- [ ] PostgreSQL lokal setup + `CREATE DATABASE academate_dev`

### Addım 2 — Domain Layer
- [ ] `BaseEntity` (id UUID, createdAt, updatedAt, `@MappedSuperclass`)
- [ ] Enum-lar: `Role`, `Subject`, `ContentType`, `ContentStatus`, `AiMessageRole`, `RelationType`, `AuditAction`
- [ ] Entity-lər (14 ədəd) + `@Enumerated(EnumType.STRING)`
- [ ] Repository-lər (`JpaRepository`)

### Addım 3 — Security Core
- [ ] `JwtProperties` (`@ConfigurationProperties("app.jwt")`)
- [ ] `JwtService` — generate/parse/validate
- [ ] `CustomUserDetails` + `CustomUserDetailsService`
- [ ] `JwtAuthenticationFilter`
- [ ] `JwtAuthenticationEntryPoint` + `AccessDeniedHandler`
- [ ] `SecurityConfig` — FilterChain, PasswordEncoder, AuthenticationManager
- [ ] `SecurityUtils.getCurrentUserId()`

### Addım 4 — Exception Handling
- [ ] `ApiException` hierarchy
- [ ] `ApiError` DTO (RFC 7807)
- [ ] `GlobalExceptionHandler` (`@RestControllerAdvice`)
- [ ] Validation error formatter

### Addım 5 — Auth Module
- [ ] DTO-lar (request/response)
- [ ] `RefreshTokenService` (generate, validate, rotate, revoke)
- [ ] `EmailVerificationService`
- [ ] `PasswordResetService`
- [ ] `AuthService` (register, login, refresh, logout)
- [ ] `AuthController`
- [ ] Custom validator: `@StrongPassword`, `@ValidAzerbaijaniPhone`

### Addım 6 — Audit & Rate Limiting
- [ ] `AuditLog` entity + repository
- [ ] `AuditLogService` (async)
- [ ] Auth event listener
- [ ] `RateLimitFilter` + Bucket4j konfiqurasiyası

### Addım 7 — Email Infrastructure
- [ ] `EmailService` (send verification, reset link)
- [ ] HTML templates (Thymeleaf və ya plain)
- [ ] Dev mühitdə MailHog və ya log-only mode

### Addım 8 — S3 Integration (ilkin setup)
- [ ] `S3Config` + `S3Properties`
- [ ] `S3StorageService` (generate pre-signed URL, delete)
- [ ] Content modulunda istifadə üçün hazır

### Addım 9 — Test & Docs
- [ ] Unit testlər: `JwtService`, `AuthService`
- [ ] Integration testlər: auth flow (Testcontainers + PostgreSQL)
- [ ] Swagger UI konfiqurasiyası (`/swagger-ui.html`)
- [ ] `README.md` — setup, run, env variables
- [ ] Postman collection export

---

## 8. application.yml Nümunəsi (şablon)

```yaml
spring:
  application:
    name: academate-backend
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/academate_dev}
    username: ${DB_USER:postgres}
    password: ${DB_PASS:postgres}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        jdbc.time_zone: UTC
        format_sql: true
    open-in-view: false
  mail:
    host: ${SMTP_HOST:localhost}
    port: ${SMTP_PORT:1025}
    username: ${SMTP_USER:}
    password: ${SMTP_PASS:}
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 500MB

server:
  port: 8080
  error:
    include-message: always
    include-stacktrace: never

app:
  jwt:
    secret: ${JWT_SECRET}
    access-token-expiration: PT15M
    refresh-token-expiration: P7D
    issuer: academate
  cors:
    allowed-origins: ${CORS_ORIGINS:http://localhost:3000,http://localhost:5173}
  s3:
    region: ${AWS_REGION:eu-central-1}
    bucket: ${AWS_S3_BUCKET:academate-dev}
    access-key: ${AWS_ACCESS_KEY}
    secret-key: ${AWS_SECRET_KEY}
  ai-service:
    base-url: ${AI_SERVICE_URL:http://localhost:8000}

springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs
```

---

## 9. MVP-də Olmayanlar (Post-MVP)

- Flyway migration (manual SQL ilə başlayırıq, sonra köçəcək)
- OAuth2 / Google Sign-In
- 2FA (TOTP)
- Redis-based rate limit və session
- WebSocket (real-time chat üçün)
- RBAC permission-based model
- Teacher verification workflow UI
- Admin panel
- Quiz system
- Payment integration
- Notification service (push, SMS)
- Analytics / reporting

---

## 10. Qeydlər & Xəbərdarlıqlar

- **MVP-də `ddl-auto=update`** istifadə olunur — Hibernate entity-lərdən cədvəlləri avtomatik yaradır və yeni sahələri əlavə edir. Sadəlik üçün seçilib.
- **Diqqət:** `update` column silmir, tip dəyişdirmir, rename etmir. Post-MVP-də Flyway və `validate` rejiminə keçiləcək.
- **JWT secret** heç vaxt commit edilməməlidir. `.gitignore`-a `.env` əlavə et.
- **S3 credentials** production-da IAM role istifadə edilməlidir (env-dən deyil).
- **Email təsdiqi** olmadan login edə bilmə — MVP-də soft (xəbərdarlıq göstər), post-MVP-də sərt.
- **AI service (Python RAG)** ayrıca mikroservisdir — backend yalnız HTTP client ilə çağırır, token quota backend tərəfdə yoxlanılır.
- **Telefon validation** — Azerbaycan formatı: `+994XXXXXXXXX` (custom validator).

---

## 11. Növbəti Addım

Plan tam təsdiqlənib. **Addım 1** (Gradle dependencies + application.yml + env setup) ilə başlayırıq.
