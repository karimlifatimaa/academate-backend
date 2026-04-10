# Academate Backend — Tam Funksionallıq Sənədi

> **Platforma:** Azərbaycan məktəb şagirdləri üçün yerli AI təhsil platforması
> **Stack:** Spring Boot 3.5 · Java 17 · PostgreSQL · DigitalOcean Spaces (S3) · JWT
> **Base URL:** `http://localhost:8080/api/v1`
> **Swagger UI:** `http://localhost:8080/swagger-ui.html`

---

## İstifadəçi Rolları

| Rol | Kim? | Əsas məqsəd |
|-----|------|-------------|
| `STUDENT` | Məktəb şagirdi | Kontent izlə, tərəqqi et, valideynə kodu ver |
| `TEACHER` | Müəllim | Kontent yüklə, dərs materialı paylaş |
| `PARENT` | Valideyn | Uşağın tərəqqini izlə |
| `ADMIN` | Sistem admini | Platformanı idarə et, müəllimləri təsdiqlə |

---

## Fənn Siyahısı (`Subject`)

| Dəyər | Fənn |
|-------|------|
| `RIYAZIYYAT` | Riyaziyyat |
| `FIZIKA` | Fizika |
| `KIMYA` | Kimya |
| `BIOLOGIYA` | Biologiya |
| `INFORMATIKA` | İnformatika |
| `AZERBAYCAN_DILI` | Azərbaycan dili |
| `EDEBIYYAT` | Ədəbiyyat |
| `INGILIS_DILI` | İngilis dili |
| `TARIX` | Tarix |
| `COGRAFIYA` | Coğrafiya |

---

## 1. Auth Modulu — `/api/v1/auth`

### 1.1 Şagird Qeydiyyatı

```
POST /api/v1/auth/register/student
```

**Request body:**
```json
{
  "fullName": "Əli Həsənov",
  "email": "ali@gmail.com",
  "password": "Parol123!",
  "phone": "+994501234567",
  "grade": 10,
  "schoolName": "239 saylı məktəb",
  "city": "Bakı",
  "birthDate": "2008-05-15"
}
```

**Response `201`:**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "expiresIn": 900,
  "user": {
    "id": "uuid",
    "fullName": "Əli Həsənov",
    "email": "ali@gmail.com",
    "username": "ali_hesenov",
    "role": "STUDENT",
    "avatarUrl": null,
    "emailVerified": false
  }
}
```

> Qeydiyyatdan sonra email gəlir, link ilə hesab təsdiqlənir.

---

### 1.2 Müəllim Qeydiyyatı

```
POST /api/v1/auth/register/teacher
```

**Request body:**
```json
{
  "fullName": "Nigar Əliyeva",
  "email": "nigar@gmail.com",
  "password": "Parol123!",
  "phone": "+994501234567",
  "subjects": ["RIYAZIYYAT", "FIZIKA"]
}
```

**Response `201`:** — Auth cavabı (yuxarıdakı kimi)

---

### 1.3 Valideyn Qeydiyyatı

```
POST /api/v1/auth/register/parent
```

**Request body:**
```json
{
  "fullName": "Fatimə Kərimli",
  "email": "fatima@gmail.com",
  "password": "Parol123!",
  "phone": "+994501234567"
}
```

**Response `201`:** — Auth cavabı

---

### 1.4 Uşaq Qeydiyyatı (Valideyn tərəfindən)

```
POST /api/v1/auth/register/child
Authorization: Bearer {token}   (yalnız PARENT)
```

**Request body:**
```json
{
  "fullName": "Murad Kərimli",
  "grade": 5,
  "schoolName": "12 saylı məktəb",
  "birthDate": "2013-03-20"
}
```

> Valideyn öz uşağı üçün hesab açır — email olmadan.

---

### 1.5 Giriş

```
POST /api/v1/auth/login
```

**Request body:**
```json
{
  "identifier": "ali@gmail.com",
  "password": "Parol123!"
}
```

> `identifier` — email və ya username ilə daxil olmaq olar.

**Response `200`:** — Auth cavabı (accessToken + refreshToken)

---

### 1.6 Token Yenilə

```
POST /api/v1/auth/refresh
```

**Request body:**
```json
{
  "refreshToken": "eyJ..."
}
```

**Response `200`:** — Yeni accessToken + refreshToken

---

### 1.7 Çıxış

```
POST /api/v1/auth/logout
Authorization: Bearer {token}
```

**Request body:**
```json
{
  "refreshToken": "eyJ..."
}
```

> Refresh token DB-dən silinir, artıq istifadə edilə bilməz.

---

### 1.8 Email Təsdiqi

```
GET /api/v1/auth/verify-email?token=abc123...
```

> Emaildəki link bu endpoint-ə yönləndirir. Uğurlu olduqda hesab `emailVerified = true` olur.

---

### 1.9 Doğrulama Emailini Yenidən Göndər

```
POST /api/v1/auth/resend-verification
Authorization: Bearer {token}
```

---

### 1.10 Parolu Dəyiş

```
POST /api/v1/auth/change-password
Authorization: Bearer {token}
```

**Request body:**
```json
{
  "currentPassword": "KöhnəParol123!",
  "newPassword": "YeniParol456!"
}
```

---

### 1.11 Cari İstifadəçi

```
GET /api/v1/auth/me
Authorization: Bearer {token}
```

**Response `200`:**
```json
{
  "id": "uuid",
  "fullName": "Əli Həsənov",
  "email": "ali@gmail.com",
  "username": "ali_hesenov",
  "role": "STUDENT",
  "avatarUrl": "https://...",
  "emailVerified": true
}
```

---

### Auth — Xəta Kodları

| Kod | Səbəb |
|-----|-------|
| `401` | Token yanlış və ya bitib |
| `403` | Bu əməliyyat üçün icazə yoxdur |
| `409` | Email artıq qeydiyyatdadır |
| `400` | Validation xətası |

---

## 2. İstifadəçi Modulu — `/api/v1/users`

### 2.1 Öz Profilini Gör

```
GET /api/v1/users/me/profile
Authorization: Bearer {token}
```

**Response `200` — STUDENT:**
```json
{
  "id": "uuid",
  "fullName": "Əli Həsənov",
  "email": "ali@gmail.com",
  "username": "ali_hesenov",
  "phone": "+994501234567",
  "avatarUrl": "https://...",
  "preferredLanguage": "az",
  "role": "STUDENT",
  "emailVerified": true,
  "grade": 10,
  "schoolName": "239 saylı məktəb",
  "city": "Bakı",
  "birthDate": "2008-05-15"
}
```

**Response `200` — TEACHER:**
```json
{
  "id": "uuid",
  "fullName": "Nigar Əliyeva",
  "email": "nigar@gmail.com",
  "role": "TEACHER",
  "emailVerified": true,
  "bio": "10 illik təcrübəli riyaziyyat müəllimi",
  "hourlyRate": 25.00,
  "rating": 4.8,
  "isVerified": true,
  "subjects": ["RIYAZIYYAT", "FIZIKA"]
}
```

**Response `200` — PARENT:**
```json
{
  "id": "uuid",
  "fullName": "Fatimə Kərimli",
  "email": "fatima@gmail.com",
  "role": "PARENT",
  "emailVerified": true,
  "occupation": "Mühasib"
}
```

---

### 2.2 Öz Profilini Yenilə

```
PATCH /api/v1/users/me
Authorization: Bearer {token}
```

**Request body (hamısı optional):**
```json
{
  "fullName": "Əli Həsənov",
  "phone": "+994501234567",
  "preferredLanguage": "az"
}
```

**Response `200`:** — UserResponse (id, fullName, email, username, role, avatarUrl, emailVerified)

---

### 2.3 Şagird Profilini Yenilə

```
PATCH /api/v1/users/me/student-profile
Authorization: Bearer {token}   (yalnız STUDENT)
```

**Request body (hamısı optional):**
```json
{
  "grade": 11,
  "schoolName": "Heydər Əliyev adına məktəb",
  "city": "Gəncə"
}
```

**Response `204`:** No Content

---

### 2.4 Müəllim Profilini Yenilə

```
PATCH /api/v1/users/me/teacher-profile
Authorization: Bearer {token}   (yalnız TEACHER)
```

**Request body (hamısı optional):**
```json
{
  "bio": "10 illik təcrübəli riyaziyyat müəllimi",
  "hourlyRate": 30.00
}
```

**Response `204`:** No Content

---

### 2.5 Avatar Yüklə

```
POST /api/v1/users/me/avatar?fileName=photo.jpg&contentType=image/jpeg
Authorization: Bearer {token}
```

**Response `200`:**
```json
{
  "uploadUrl": "https://fra1.digitaloceanspaces.com/...?X-Amz-Signature=...",
  "avatarUrl": "https://fra1.digitaloceanspaces.com/avatars/uuid.jpg"
}
```

**Necə istifadə edilir:**
1. Bu endpoint-dən `uploadUrl` al
2. `PUT {uploadUrl}` — body-də fayl, header-da `Content-Type: image/jpeg`
3. Fayl S3-ə yüklənir, `avatarUrl` DB-ə yazılır

---

### 2.6 Hesabı Sil (Soft Delete)

```
DELETE /api/v1/users/me
Authorization: Bearer {token}
```

**Response `204`:** No Content

> Hesab DB-dən silinmir. `deletedAt` timestamp yazılır, `isActive = false` olur.

---

### 2.7 Müəllimin İctimai Profili

```
GET /api/v1/users/teachers/{id}
```

> Token tələb olunmur — hər kəs görə bilər.

**Response `200`:**
```json
{
  "id": "uuid",
  "fullName": "Nigar Əliyeva",
  "avatarUrl": "https://...",
  "bio": "10 illik təcrübəli riyaziyyat müəllimi",
  "hourlyRate": 25.00,
  "rating": 4.8,
  "isVerified": true,
  "subjects": ["RIYAZIYYAT", "FIZIKA"]
}
```

---

## 3. Kontent Modulu — `/api/v1/content`

### Kontent Tipləri (`ContentType`)

| Dəyər | İzah |
|-------|------|
| `VIDEO` | Video dərs |
| `PDF` | PDF sənəd |
| `PPTX` | PowerPoint |
| `DOCUMENT` | Word/text sənəd |
| `QUIZ` | Test (gələcəkdə) |

### Kontent Statusları (`ContentStatus`)

| Dəyər | İzah |
|-------|------|
| `DRAFT` | Qaralama — yalnız müəllim görür |
| `PUBLISHED` | Dərc edilib — hamı görür |
| `ARCHIVED` | Arxivlənib |
| `FLAGGED` | Admin tərəfindən qeyd edilib |

---

### 3.1 Dərc Edilmiş Kontentləri Listele

```
GET /api/v1/content?subject=RIYAZIYYAT&grade=10&page=0&size=20
```

> Token tələb olunmur.

**Query parametrləri:**

| Parametr | Tip | İzah |
|----------|-----|------|
| `subject` | `Subject` | Fənn filtri (optional) |
| `grade` | `Short` | Sinif filtri 1–11 (optional) |
| `page` | `int` | Səhifə nömrəsi (default: 0) |
| `size` | `int` | Hər səhifədə nəticə (default: 20) |

**Response `200`:**
```json
{
  "content": [
    {
      "id": "uuid",
      "title": "Kvadrat tənliklər — giriş",
      "description": "Kvadrat tənliklərin həll üsulları",
      "subject": "RIYAZIYYAT",
      "grade": 10,
      "topic": "Cəbr",
      "type": "VIDEO",
      "status": "PUBLISHED",
      "fileUrl": "https://...",
      "isFree": true,
      "viewCount": 142,
      "teacherId": "uuid",
      "createdAt": "2024-09-01T10:00:00Z"
    }
  ],
  "totalElements": 54,
  "totalPages": 3,
  "number": 0
}
```

---

### 3.2 Kontenin Detalları

```
GET /api/v1/content/{id}
```

> Token tələb olunmur. Hər baxışda `viewCount` bir artır.

**Response `200`:** — Tam ContentResponse (yuxarıdakı kimi)

---

### 3.3 Yeni Kontent Yarat

```
POST /api/v1/content
Authorization: Bearer {token}   (yalnız TEACHER)
```

**Request body:**
```json
{
  "title": "Kvadrat tənliklər — giriş",
  "description": "Kvadrat tənliklərin həll üsulları izah edilir",
  "subject": "RIYAZIYYAT",
  "grade": 10,
  "topic": "Cəbr",
  "type": "VIDEO",
  "isFree": true
}
```

**Response `201`:** — ContentResponse (`status: "DRAFT"`)

---

### 3.4 Fayl Upload URL Al

```
POST /api/v1/content/{id}/upload-url?fileName=video.mp4&contentType=video/mp4
Authorization: Bearer {token}   (yalnız TEACHER — öz kontenini)
```

**Response `200`:**
```json
{
  "uploadUrl": "https://fra1.digitaloceanspaces.com/...?X-Amz-Signature=...",
  "key": "content/uuid/video.mp4"
}
```

**Fayl yükləmə axını:**
1. `POST /upload-url` → `uploadUrl` al
2. `PUT {uploadUrl}` (fayl binary, doğru Content-Type header)
3. Fayl S3-də saxlanır, kontent `fileUrl` ilə yenilənir

---

### 3.5 Öz Kontentlərini Gör

```
GET /api/v1/content/my?page=0&size=20
Authorization: Bearer {token}   (yalnız TEACHER)
```

> DRAFT daxil bütün statusları qaytarır.

**Response `200`:** — Paginated ContentResponse siyahısı

---

### 3.6 Kontenini Redaktə Et

```
PATCH /api/v1/content/{id}
Authorization: Bearer {token}   (yalnız TEACHER — öz kontenini)
```

**Request body (hamısı optional):**
```json
{
  "title": "Yeni başlıq",
  "description": "Yeni açıqlama",
  "isFree": false,
  "status": "ARCHIVED"
}
```

**Response `200`:** — Yenilənmiş ContentResponse

---

### 3.7 Kontenini Dərc Et

```
POST /api/v1/content/{id}/publish
Authorization: Bearer {token}   (yalnız TEACHER — öz kontenini)
```

**Response `200`:** — ContentResponse (`status: "PUBLISHED"`)

> DRAFT → PUBLISHED. Bundan sonra şagirdlər görə bilər.

---

### 3.8 Konteniti Sil

```
DELETE /api/v1/content/{id}
Authorization: Bearer {token}   (TEACHER öz kontenini / ADMIN hər şeyi)
```

**Response `204`:** No Content

---

## 4. Tərəqqi Modulu — `/api/v1/progress`

### 4.1 Öz Tərəqqini Gör

```
GET /api/v1/progress/me
Authorization: Bearer {token}   (yalnız STUDENT)
```

**Response `200`:**
```json
[
  {
    "subject": "RIYAZIYYAT",
    "masteryScore": 78.5,
    "questionsAsked": 45,
    "correctAnswers": 35
  },
  {
    "subject": "FIZIKA",
    "masteryScore": 62.0,
    "questionsAsked": 20,
    "correctAnswers": 12
  }
]
```

| Sahə | İzah |
|------|------|
| `masteryScore` | Fənn üzrə mənimsəmə faizi (0–100) |
| `questionsAsked` | Verilmiş sual sayı |
| `correctAnswers` | Düzgün cavab sayı |

---

### 4.2 Uşağın Tərəqqini Gör

```
GET /api/v1/progress/children/{childId}
Authorization: Bearer {token}   (yalnız PARENT)
```

> Yalnız əlaqəli (linked) uşağın məlumatı görünür. Əlaqə yoxdursa `403` qaytarır.

**Response `200`:** — ProgressResponse siyahısı (yuxarıdakı kimi)

---

## 5. Ailə Modulu — `/api/v1/family`

Valideyn-Şagird əlaqəsi sistemi.

### Əlaqə Tipləri (`RelationType`)

| Dəyər | İzah |
|-------|------|
| `MOTHER` | Ana |
| `FATHER` | Ata |
| `GUARDIAN` | Qəyyum |

---

### 5.1 Dəvət Kodu Yarat (Şagird)

```
POST /api/v1/family/invite-code
Authorization: Bearer {token}   (yalnız STUDENT)
```

**Response `200`:**
```json
{
  "code": "A3F8K2",
  "expiresAt": "2024-09-02T10:00:00Z"
}
```

> Kod 6 simvollu, müddəti bitənə qədər istifadə edilə bilər.

---

### 5.2 Kodla Uşağa Bağlan (Valideyn)

```
POST /api/v1/family/link
Authorization: Bearer {token}   (yalnız PARENT)
```

**Request body:**
```json
{
  "code": "A3F8K2",
  "relationType": "MOTHER"
}
```

**Response `200`:** — Uğurlu əlaqə mesajı

> Əlaqə qurulduqdan sonra valideyn uşağın tərəqqini görə bilər.

---

### 5.3 Bağlı Uşaqları Gör

```
GET /api/v1/family/children
Authorization: Bearer {token}   (yalnız PARENT)
```

**Response `200`:**
```json
[
  {
    "id": "uuid",
    "fullName": "Murad Kərimli",
    "email": "murad@gmail.com",
    "username": "murad_kerimli",
    "role": "STUDENT",
    "avatarUrl": null,
    "emailVerified": true
  }
]
```

---

### 5.4 Bağlı Valideynləri Gör

```
GET /api/v1/family/parents
Authorization: Bearer {token}   (yalnız STUDENT)
```

**Response `200`:** — UserResponse siyahısı (valideynlər)

---

### Ailə Modulu — Tam Axın

```
1. Şagird → POST /family/invite-code → "A3F8K2" kodu alır
2. Kodu valideynə verir (şifahi, WhatsApp, SMS...)
3. Valideyn → POST /family/link {"code": "A3F8K2", "relationType": "MOTHER"}
4. Əlaqə quruldu ✓
5. Valideyn → GET /progress/children/{childId} → uşağın tərəqqisi
```

---

## 6. Admin Modulu — `/api/v1/admin`

> **Bütün endpointlər yalnız `ADMIN` rolu üçündür**

### 6.1 Bütün İstifadəçiləri Listele

```
GET /api/v1/admin/users?page=0&size=20
Authorization: Bearer {token}   (yalnız ADMIN)
```

**Response `200`:**
```json
{
  "content": [
    {
      "id": "uuid",
      "fullName": "Əli Həsənov",
      "email": "ali@gmail.com",
      "username": "ali_hesenov",
      "role": "STUDENT",
      "avatarUrl": null,
      "emailVerified": true
    }
  ],
  "totalElements": 120,
  "totalPages": 6
}
```

---

### 6.2 Müəllimləri Listele

```
GET /api/v1/admin/teachers?page=0&size=20
Authorization: Bearer {token}   (yalnız ADMIN)
```

**Response `200`:**
```json
{
  "content": [
    {
      "userId": "uuid",
      "fullName": "Nigar Əliyeva",
      "email": "nigar@gmail.com",
      "phone": "+994501234567",
      "isVerified": false
    }
  ]
}
```

---

### 6.3 Müəllimi Təsdiqlə

```
POST /api/v1/admin/teachers/{id}/verify
Authorization: Bearer {token}   (yalnız ADMIN)
```

**Response `204`:** No Content

> Müəllim `isVerified = true` olur. Profilində "Təsdiqlənmiş müəllim" badge göstərilir.

---

### 6.4 İstifadəçini Deaktiv Et

```
POST /api/v1/admin/users/{id}/deactivate
Authorization: Bearer {token}   (yalnız ADMIN)
```

**Response `204`:** No Content

> İstifadəçi `isActive = false` olur, login edə bilməz.

---

## 7. Audit Log — Qeydə Alınan Əməliyyatlar

Hər əhəmiyyətli əməliyyat avtomatik olaraq DB-ə yazılır:

| Əməliyyat | Nə vaxt |
|-----------|---------|
| `LOGIN_SUCCESS` | Uğurlu giriş |
| `LOGIN_FAILED` | Uğursuz giriş cəhdi |
| `LOGOUT` | Çıxış |
| `REGISTER` | Yeni qeydiyyat |
| `EMAIL_VERIFIED` | Email təsdiqi |
| `PASSWORD_CHANGED` | Parol dəyişikliyi |
| `PASSWORD_RESET_REQUESTED` | Şifrə sıfırlama sorğusu |
| `PASSWORD_RESET_COMPLETED` | Şifrə sıfırlandı |
| `ROLE_CHANGED` | Rol dəyişdirildi |
| `USER_DEACTIVATED` | İstifadəçi deaktiv edildi |
| `TEACHER_VERIFIED` | Müəllim təsdiqləndi |
| `CONTENT_CREATED` | Kontent yaradıldı |
| `CONTENT_PUBLISHED` | Kontent dərc edildi |
| `CONTENT_DELETED` | Kontent silindi |
| `PARENT_LINKED` | Valideyn-uşaq əlaqəsi quruldu |

> Hər qeyddə: istifadəçi ID, əməliyyat, IP ünvanı, cihaz məlumatı, timestamp saxlanır.

---

## 8. Texniki Funksionallıqlar

### Rate Limiting (Bucket4j)
- Hər IP ünvanı üçün müəyyən vaxt intervalında sorğu limiti var
- Limit keçildikdə `429 Too Many Requests` qaytarır
- DDoS və brute-force hücumlardan qoruyur

### JWT Autentifikasiyası
- **Access Token:** Qısa ömürlü (15 dəqiqə) — hər sorğuya `Authorization: Bearer {token}` header-ı
- **Refresh Token:** Uzun ömürlü — DB-də SHA-256 hash ilə saxlanır
- Token bitdikdə `/auth/refresh` ilə yenilənir

### Fayl Yükləmə (DigitalOcean Spaces)
- Müəllim avatar və kontent faylları burada saxlanır
- Pre-signed URL: backend URL yaradır, frontend birbaşa S3-ə yükləyir (backend keçmir)
- Qovluq strukturu: `avatars/{userId}.jpg`, `content/{contentId}/{fileName}`

### Email Sistemi (Gmail SMTP)
- HTML şablonlar — Azərbaycan dilində
- Asinxron göndəriş (`@Async`) — sorğu bloklanmır
- Hallar: email təsdiqi, parol sıfırlama

### Soft Delete
- İstifadəçilər DB-dən heç vaxt silinmir
- `deletedAt` timestamp yazılır, `isActive = false` olur
- Audit log saxlanır

### Admin Seed (Startup)
- App ilk başlayanda admin hesabı avtomatik yaranır
- Email: `kerimlifatime417@gmail.com`
- Rol: `ADMIN`, `emailVerified = true`

---

## 9. Ümumi Xəta Formatı (RFC 7807)

Bütün xətalar eyni formatda qaytarılır:

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Token etibarsız və ya müddəti bitib",
  "instance": "/api/v1/auth/me"
}
```

| HTTP Kodu | Mənası |
|-----------|--------|
| `400` | Validation xətası — məlumat düzgün deyil |
| `401` | Token yoxdur, yanlışdır, ya bitib |
| `403` | Bu əməliyyat üçün icazəniz yoxdur |
| `404` | Resurs tapılmadı |
| `409` | Conflict — məsələn, email artıq var |
| `429` | Çox sorğu — rate limit aşıldı |
| `500` | Server xətası |

---

## 10. Gələcək Modullar (MVP-dən sonra)

| Modul | İzah | Prioritet |
|-------|------|-----------|
| **AI Sual-Cavab** | RAG əsaslı müəllim köməkçisi — Azərbaycan dilində | Yüksək |
| **Quiz Sistemi** | Test sualları, avtomatik qiymətləndirmə, nəticə izlənməsi | Yüksək |
| **Push Bildirişlər** | Yeni kontent, tərəqqi xəbərdarlıqları | Orta |
| **Cədvəl/Booking** | Şagird-müəllim dərs planlaması | Orta |
| **Ödəniş İnteqrasiyası** | Müəllim saatlıq ödəniş, premium kontent | Orta |
| **Soft Delete Filter** | Silinmiş istifadəçilər sorğulardan çıxarılsın | Aşağı |
| **Curriculum Topics** | DB-də fənn mövzuları (Azərbaycan tədris proqramı) | Aşağı |
