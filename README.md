# 🎫 B2B ITSM Ticketing System — Backend API

Sistem ticketing untuk manajemen IT Service Management (ITSM) berbasis B2B. Backend dibangun dengan Spring Boot dan PostgreSQL, mendukung **JWT Authentication**, **Role-Based Access Control (RBAC)** dengan empat tingkat role (ADMIN, SUPPORT, TECHNICAL_SUPPORT, USER), **alur eskalasi tiket Tier 1 → Tier 2**, serta **Real-time Chat** via WebSocket.

---

## 📋 Daftar Isi

- [Tech Stack](#-tech-stack)
- [Prasyarat](#-prasyarat)
- [Cara Menjalankan](#-cara-menjalankan)
- [Konfigurasi](#-konfigurasi)
- [Authentication](#-authentication)
- [Role-Based Access Control](#-role-based-access-control)
- [API Endpoints](#-api-endpoints)
  - [Auth API](#-auth-api)
  - [User API](#-user-api)
  - [Client API](#-client-api)
  - [Client Support API](#-client-support-api-admin-only)
  - [Project API](#-project-api)
  - [Client Quota API](#-client-quota-api)
  - [My Quota API](#-my-quota-api-admin--user)
  - [Service Catalog API](#-service-catalog-api-admin-only)
  - [Ticket API](#-ticket-api)
  - [Ticket Assignment API](#-ticket-assignment-api)
  - [SLA Report API](#-sla-report-api-admin--user)
  - [Chat API (WebSocket + REST)](#-chat-api-websocket--rest)
  - [Chat Attachment API](#-chat-attachment-api)
  - [Notification API](#-notification-api)
  - [Ticket Worklog API](#-ticket-worklog-api)
- [Enum Reference](#-enum-reference)
- [Error Handling](#-error-handling)

---

## 🛠 Tech Stack

| Teknologi | Versi |
|-----------|-------|
| Java | 22 |
| Spring Boot | 3.2.5 |
| Spring Security | 6.x |
| JWT (JJWT) | 0.12.6 |
| Spring WebSocket | 3.2.x |
| Spring Data JPA | 3.2.x |
| PostgreSQL | 15+ |
| Lombok | Latest |
| Maven | 3.9+ |

---

## 📌 Prasyarat

- **JDK 22** atau lebih baru
- **Maven 3.9+**
- **PostgreSQL** database (atau Supabase)

---

## 🚀 Cara Menjalankan

```bash
# 1. Clone repository
git clone https://github.com/calpadia/ticketing-springboot-api.git
cd ticketing-springboot-api

# 2. Konfigurasi database di application.properties

# 3. Jalankan aplikasi
mvn spring-boot:run
```

Aplikasi akan berjalan di `http://localhost:8082`

---

## ⚙ Konfigurasi

Edit file `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/ticketing
spring.datasource.username=postgres
spring.datasource.password=your_password

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.show-sql=true

# Server
server.port=8082

# JWT Configuration
jwt.secret=<your-base64-encoded-256-bit-secret-key>
jwt.expiration=7200000

# CORS allowed origins (comma-separated)
security.cors.allowed-origins=http://localhost:3000,http://localhost:5173

# File upload
file.upload-dir=./uploads
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=50MB
```

---

## 🔐 Authentication

Sistem menggunakan **JWT (JSON Web Token)** untuk autentikasi stateless.

### Alur Autentikasi

```
1. Register/Login  →  POST /api/v1/auth/register atau /login
2. Dapatkan Token  →  Response berisi JWT token
3. Gunakan Token   →  Header: Authorization: Bearer <token>
4. Akses API       →  Server validasi token + cek role
```

### Cara Menggunakan Token

Setelah login/register, tambahkan header berikut di setiap request:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

Token berlaku **2 jam** (dapat dikonfigurasi via `jwt.expiration`).

---

## 🛡 Role-Based Access Control

### Role yang Tersedia

| Role | Tier | Deskripsi |
|------|------|-----------|
| `ADMIN` | — | Full access ke semua fitur sistem. Bisa CRUD user/client/quota/project, assign ticket ke siapa saja. |
| `SUPPORT` | Tier 1 | Customer-facing support. Auto-assigned ke ticket dari client-nya, bisa **eskalasi** ticket ke `TECHNICAL_SUPPORT`. |
| `TECHNICAL_SUPPORT` | Tier 2/3 | Technical engineer. Hanya menerima ticket eskalasi dari SUPPORT/ADMIN dan mengerjakan ticket yang di-assign. |
| `USER` | — | Client user. Bisa create ticket, melihat ticket milik client-nya, dan chat. |

### Alur Eskalasi Tiket

```
USER buat ticket
    ↓
SUPPORT (Tier 1) auto-assigned (via client-supports)
    ↓
SUPPORT eskalasi ke TECHNICAL_SUPPORT (Tier 2)  ← POST /api/v1/tickets/{id}/assign
    ↓
TECHNICAL_SUPPORT mengerjakan ticket
```

### Access Control Matrix

| Endpoint | ADMIN | SUPPORT | TECHNICAL_SUPPORT | USER | Public |
|----------|-------|---------|-------------------|------|--------|
| `POST /api/v1/auth/register` | — | — | — | — | ✅ |
| `POST /api/v1/auth/login` | — | — | — | — | ✅ |
| `GET/POST/PUT/DELETE /api/v1/users/**` | ✅ | ❌ | ❌ | ❌ | ❌ |
| `GET /api/v1/users/assignable` | ✅ | ✅ | ❌ | ❌ | ❌ |
| `GET/POST/PUT/DELETE /api/v1/clients/**` | ✅ | ❌ | ❌ | ❌ | ❌ |
| `POST/DELETE/GET /api/v1/clients/{id}/supports` | ✅ | ❌ | ❌ | ❌ | ❌ |
| `POST/PUT/DELETE /api/v1/projects/**` | ✅ | ❌ | ❌ | ❌ | ❌ |
| `GET /api/v1/projects/**` | ✅ | ❌ | ❌ | ✅ (own client) | ❌ |
| `GET/POST/PUT/DELETE /api/v1/client-quotas/**` | ✅ | ❌ | ❌ | ❌ | ❌ |
| `GET /api/v1/my-quotas/**` | ✅ | ❌ | ❌ | ✅ | ❌ |
| `GET/POST/PUT/DELETE /api/v1/service-catalogs/**` | ✅ | ❌ | ❌ | ❌ | ❌ |
| `GET /api/v1/sla-report/**` | ✅ | ❌ | ❌ | ✅ (own client) | ❌ |
| `POST /api/v1/tickets` | ✅ | ❌ | ❌ | ✅ | ❌ |
| `GET /api/v1/tickets/**` | ✅ | ✅ (assigned) | ✅ (assigned) | ✅ (own client) | ❌ |
| `PUT /api/v1/tickets/{id}/status` | ✅ | ✅ | ✅ | ✅ | ❌ |
| `POST /api/v1/tickets/{id}/assign` | ✅ (→ siapa saja) | ✅ (→ TECHNICAL_SUPPORT) | ❌ | ❌ | ❌ |
| `POST /api/v1/tickets/{id}/unassign` | ✅ | ✅ | ❌ | ❌ | ❌ |
| `POST /api/v1/tickets/{id}/reassign` | ✅ | ✅ | ❌ | ❌ | ❌ |
| `GET /api/v1/tickets/{id}/assignments` | ✅ | ✅ | ✅ | ❌ | ❌ |
| `GET /api/v1/tickets/my-assignments` | ✅ | ✅ | ✅ | ❌ | ❌ |
| `GET/POST /api/v1/chat/**` | ✅ | ✅ | ✅ | ✅ | ❌ |
| `WebSocket /ws` (handshake) | — | — | — | — | ✅ |
| `STOMP /app/chat.send` | ✅ | ✅ | ✅ | ✅ | ❌ |

---

## 📡 API Endpoints

**Base URL:** `http://localhost:8082`

### Ringkasan Seluruh API

| Method | Endpoint | Deskripsi | Auth |
|--------|----------|-----------|------|
| **AUTH** | | | |
| `POST` | `/api/v1/auth/register` | Registrasi user baru | Public |
| `POST` | `/api/v1/auth/login` | Login & dapatkan token | Public |
| **USER** | | | |
| `POST` | `/api/v1/users` | Buat user baru | ADMIN |
| `GET` | `/api/v1/users` | Ambil semua user | ADMIN |
| `GET` | `/api/v1/users/assignable` | Daftar engineer yang bisa di-assign (filter per role) | ADMIN, SUPPORT |
| `GET` | `/api/v1/users/{id}` | Ambil user by ID | ADMIN |
| `PUT` | `/api/v1/users/{id}` | Update user | ADMIN |
| `DELETE` | `/api/v1/users/{id}` | Hapus user | ADMIN |
| **CLIENT** | | | |
| `POST` | `/api/v1/clients` | Buat client baru | ADMIN |
| `GET` | `/api/v1/clients` | Ambil semua client | ADMIN |
| `GET` | `/api/v1/clients/{id}` | Ambil client by ID | ADMIN |
| `PUT` | `/api/v1/clients/{id}` | Update client | ADMIN |
| `PATCH` | `/api/v1/clients/{id}/status` | Activate/deactivate client | ADMIN |
| `DELETE` | `/api/v1/clients/{id}` | Hapus client | ADMIN |
| **CLIENT SUPPORT** | | | |
| `POST` | `/api/v1/clients/{id}/supports` | Tambah SUPPORT ke client | ADMIN |
| `GET` | `/api/v1/clients/{id}/supports` | Lihat SUPPORT untuk client | ADMIN |
| `DELETE` | `/api/v1/clients/{id}/supports` | Hapus SUPPORT dari client | ADMIN |
| **PROJECT** | | | |
| `POST` | `/api/v1/projects` | Buat project baru | ADMIN |
| `GET` | `/api/v1/projects` | Ambil semua project | ADMIN, USER |
| `GET` | `/api/v1/projects/{id}` | Ambil project by ID | ADMIN, USER |
| `GET` | `/api/v1/projects/client/{clientId}` | Project per client | ADMIN, USER |
| `PUT` | `/api/v1/projects/{id}` | Update project | ADMIN |
| `DELETE` | `/api/v1/projects/{id}` | Hapus project | ADMIN |
| **CLIENT QUOTA** | | | |
| `POST` | `/api/v1/client-quotas` | Buat kuota baru | ADMIN |
| `GET` | `/api/v1/client-quotas` | Ambil semua kuota | ADMIN |
| `GET` | `/api/v1/client-quotas/{id}` | Ambil kuota by ID | ADMIN |
| `GET` | `/api/v1/client-quotas/client/{id}/year/{y}` | Ambil kuota by client & tahun | ADMIN |
| `PUT` | `/api/v1/client-quotas/{id}` | Update kuota | ADMIN |
| `DELETE` | `/api/v1/client-quotas/{id}` | Hapus kuota | ADMIN |
| **MY QUOTA** | | | |
| `GET` | `/api/v1/my-quotas` | Ambil kuota client sendiri | ADMIN, USER |
| `GET` | `/api/v1/my-quotas/year/{year}` | Ambil kuota client sendiri per tahun | ADMIN, USER |
| **SERVICE CATALOG** | | | |
| `POST` | `/api/v1/service-catalogs` | Buat catalog client (PM/CM) | ADMIN |
| `GET` | `/api/v1/service-catalogs` | Ambil semua catalog | ADMIN |
| `GET` | `/api/v1/service-catalogs/{id}` | Ambil catalog by ID | ADMIN |
| `GET` | `/api/v1/service-catalogs/client/{clientId}` | Ambil catalog by client | ADMIN |
| `PUT` | `/api/v1/service-catalogs/{id}` | Update services & notes | ADMIN |
| `DELETE` | `/api/v1/service-catalogs/{id}` | Hapus catalog | ADMIN |
| **TICKET** | | | |
| `POST` | `/api/v1/tickets` | Buat ticket baru (JSON / multipart) | ADMIN, USER |
| `GET` | `/api/v1/tickets` | Ambil ticket sesuai role | ADMIN, SUPPORT, TECHNICAL_SUPPORT, USER |
| `GET` | `/api/v1/tickets/{id}` | Ambil ticket by ID | ADMIN, SUPPORT, TECHNICAL_SUPPORT, USER |
| `GET` | `/api/v1/tickets/number/{no}` | Ambil ticket by nomor | ADMIN, SUPPORT, TECHNICAL_SUPPORT, USER |
| `PUT` | `/api/v1/tickets/{id}/status` | Update status ticket | ADMIN, SUPPORT, TECHNICAL_SUPPORT, USER |
| `GET` | `/api/v1/tickets/{id}/progress` | Riwayat progress ticket | ADMIN, SUPPORT, TECHNICAL_SUPPORT, USER |
| `GET` | `/api/v1/tickets/export/csv` | Export ticket ke CSV (filter: clientId, from, to) | ADMIN, SUPPORT, TECHNICAL_SUPPORT, USER |
| **SLA REPORT** | | | |
| `GET` | `/api/v1/sla-report` | Laporan SLA per client (targets + metrics) | ADMIN, USER (scoped) |
| `GET` | `/api/v1/sla-report/targets` | Tabel SLA target saja | ADMIN, USER |
| **TICKET ASSIGNMENT** | | | |
| `POST` | `/api/v1/tickets/{id}/assign` | Assign / eskalasi ticket | ADMIN, SUPPORT |
| `POST` | `/api/v1/tickets/{id}/unassign` | Unassign ticket | ADMIN, SUPPORT |
| `POST` | `/api/v1/tickets/{id}/reassign` | Reassign ticket | ADMIN, SUPPORT |
| `GET` | `/api/v1/tickets/{id}/assignments` | Lihat assignment ticket | ADMIN, SUPPORT, TECHNICAL_SUPPORT |
| `GET` | `/api/v1/tickets/my-assignments` | Ticket yang di-assign ke saya | ADMIN, SUPPORT, TECHNICAL_SUPPORT |
| **ATTACHMENT** | | | |
| `GET` | `/api/v1/attachments/ticket/{ticketId}` | Lampiran ticket | ADMIN, SUPPORT, TECHNICAL_SUPPORT, USER |
| `GET` | `/api/v1/attachments/{id}/download` | Download lampiran ticket | ADMIN, SUPPORT, TECHNICAL_SUPPORT, USER |
| **CHAT** | | | |
| `WS` | `/ws` | WebSocket handshake (SockJS) | Public |
| `STOMP` | `/app/chat.send` | Kirim pesan chat real-time | ADMIN, SUPPORT, TECHNICAL_SUPPORT, USER |
| `STOMP` | `/topic/chat/{ticketId}` | Subscribe pesan chat | ADMIN, SUPPORT, TECHNICAL_SUPPORT, USER |
| **REALTIME TICKET** | | | |
| `STOMP SUB` | `/topic/tickets/new` | Notif saat ticket baru dibuat | All authenticated |
| `STOMP SUB` | `/topic/tickets/{id}/status` | Notif saat status ticket berubah | All authenticated |
| `STOMP SUB` | `/topic/tickets/{id}/assigned` | Notif saat assignment berubah | All authenticated |
| `GET` | `/api/v1/chat/{ticketId}` | Histori chat by ticket ID | ADMIN, SUPPORT, TECHNICAL_SUPPORT, USER |
| `GET` | `/api/v1/chat/ticket/{no}` | Histori chat by ticket number | ADMIN, SUPPORT, TECHNICAL_SUPPORT, USER |
| **CHAT ATTACHMENT** | | | |
| `POST` | `/api/v1/chat/upload` | Upload file untuk chat | ADMIN, SUPPORT, TECHNICAL_SUPPORT, USER |
| `GET` | `/api/v1/chat/attachments/{id}/download` | Download file chat | ADMIN, SUPPORT, TECHNICAL_SUPPORT, USER |
| **NOTIFICATION** | | | |
| `GET` | `/api/v1/notifications/unread-count` | Jumlah notif belum dibaca | All authenticated |
| `POST` | `/api/v1/tickets/{id}/read` | Tandai ticket sudah dibaca | All authenticated |
| **WORKLOG** | | | |
| `POST` | `/api/v1/tickets/{id}/worklogs` | Mulai timer worklog | ADMIN, SUPPORT, TECHNICAL_SUPPORT |
| `GET` | `/api/v1/tickets/{id}/worklogs` | Ambil semua worklog ticket | All authenticated |
| `PUT` | `/api/v1/tickets/{id}/worklogs/{wId}/stop` | Stop timer worklog | ADMIN, SUPPORT, TECHNICAL_SUPPORT |

---

### 🔑 Auth API

#### `POST /api/v1/auth/register` — Registrasi User Baru

**Request Body:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123",
  "role": "USER"
}
```

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|------------|
| `name` | `string` | ✅ | Nama lengkap |
| `email` | `string` | ✅ | Email (harus unik) |
| `password` | `string` | ✅ | Password |
| `phone` | `string` | ❌ | Nomor telepon (optional) |
| `role` | `string` | ❌ | `ADMIN`, `SUPPORT`, `TECHNICAL_SUPPORT`, atau `USER` (default: `USER`) |
| `clientId` | `number` | ⚠ | Wajib untuk role `USER`, optional untuk role lain |

**Response — `201 Created`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "role": "USER"
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "password123",
    "role": "USER"
  }'
```

---

#### `POST /api/v1/auth/login` — Login

**Request Body:**
```json
{
  "email": "john@example.com",
  "password": "password123"
}
```

**Response — `200 OK`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "role": "USER"
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123"
  }'
```

---

### 👤 User API (ADMIN only)

> ⚠️ Semua endpoint User API memerlukan role **ADMIN**.

#### `POST /api/v1/users` — Buat User Baru

**Request Body:**
```json
{
  "name": "Jane Admin",
  "email": "jane@example.com",
  "password": "securePass",
  "role": "ADMIN"
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "name": "Jane Admin",
    "email": "jane@example.com",
    "password": "securePass",
    "role": "ADMIN"
  }'
```

#### `GET /api/v1/users` — Ambil Semua User
```bash
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/v1/users
```

#### `GET /api/v1/users/assignable` — Daftar Engineer yang Bisa Di-assign

> ✅ Endpoint khusus untuk dropdown assignment ticket. Bisa diakses **ADMIN** dan **SUPPORT**.
> Hasilnya difilter otomatis berdasarkan role caller:
> - **ADMIN** → list `SUPPORT` + `TECHNICAL_SUPPORT`
> - **SUPPORT** → list `TECHNICAL_SUPPORT` saja (alur eskalasi)
>
> Gunakan endpoint ini di frontend (bukan `GET /api/v1/users`) supaya SUPPORT bisa melihat list TECHNICAL_SUPPORT untuk eskalasi tanpa harus diberi akses penuh ke list user.

```bash
curl -H "Authorization: Bearer <SUPPORT_TOKEN>" \
  http://localhost:8080/api/v1/users/assignable
```

#### `GET /api/v1/users/{id}` — Ambil User by ID
```bash
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/v1/users/1
```

#### `PUT /api/v1/users/{id}` — Update User
```bash
curl -X PUT http://localhost:8080/api/v1/users/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"name":"Updated","email":"updated@example.com","password":"newPass","role":"USER"}'
```

#### `DELETE /api/v1/users/{id}` — Hapus User
```bash
curl -X DELETE -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/v1/users/1
```

---

### 🏢 Client API (ADMIN only)

> ⚠️ Semua endpoint Client API memerlukan role **ADMIN**.

#### `POST /api/v1/clients` — Buat Client Baru
```bash
curl -X POST http://localhost:8080/api/v1/clients \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "companyName": "PT Contoh Perusahaan",
    "contactPersonName": "Budi Santoso",
    "contactPersonEmail": "budi@contoh.com",
    "contactPersonPhone": "08123456789"
  }'
```

#### `GET /api/v1/clients` — Ambil Semua Client
```bash
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/v1/clients
```

#### `GET /api/v1/clients/{id}` — Ambil Client by ID
```bash
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/v1/clients/1
```

#### `PUT /api/v1/clients/{id}` — Update Client
```bash
curl -X PUT http://localhost:8080/api/v1/clients/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "companyName": "PT Updated",
    "contactPersonName": "Siti Rahayu",
    "contactPersonEmail": "siti@updated.com",
    "contactPersonPhone": "08198765432"
  }'
```

#### `DELETE /api/v1/clients/{id}` — Hapus Client
```bash
curl -X DELETE -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/v1/clients/1
```

#### `PATCH /api/v1/clients/{id}/status` — Aktifkan / Nonaktifkan Client

> ✅ Soft toggle — data client tetap ada, hanya flag `isActive` yang berubah. Cocok dipakai untuk tombol switch di UI.
>
> **Cascade behavior:**
> - Saat client di-**nonaktifkan** (`true → false`), semua project milik client tersebut otomatis ikut di-nonaktifkan.
> - Saat client di-**aktifkan kembali** (`false → true`), project **tidak** otomatis aktif. Admin harus restore project secara eksplisit (mencegah work dorman muncul tak sengaja).

**Request Body:**
```json
{ "isActive": false }
```

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|------------|
| `isActive` | `boolean` | ✅ | `true` untuk aktif, `false` untuk nonaktif |

```bash
curl -X PATCH http://localhost:8080/api/v1/clients/1/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{"isActive": false}'
```

---

### 👥 Client Support API (ADMIN only)

> ✅ Endpoint untuk mengelola hubungan **Client ↔ SUPPORT**.
> Setiap client bisa punya beberapa SUPPORT (Tier 1) yang otomatis di-assign saat user dari client tersebut membuat ticket baru.

#### `POST /api/v1/clients/{clientId}/supports` — Tambah SUPPORT ke Client

**Request Body:**
```json
{
  "supportUserIds": [3, 5]
}
```

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|------------|
| `supportUserIds` | `array[number]` | ✅ | List ID user dengan role `SUPPORT` |

```bash
curl -X POST http://localhost:8080/api/v1/clients/1/supports \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{"supportUserIds": [3, 5]}'
```

#### `GET /api/v1/clients/{clientId}/supports` — Lihat SUPPORT untuk Client
```bash
curl -H "Authorization: Bearer <ADMIN_TOKEN>" \
  http://localhost:8080/api/v1/clients/1/supports
```

#### `DELETE /api/v1/clients/{clientId}/supports` — Hapus SUPPORT dari Client
```bash
curl -X DELETE http://localhost:8080/api/v1/clients/1/supports \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{"supportUserIds": [3]}'
```

---

### 📁 Project API

> ✅ ADMIN dapat full CRUD. USER hanya bisa GET project milik client-nya.
> Project terkait dengan Client (1 client → many projects).

#### `POST /api/v1/projects` — Buat Project Baru (ADMIN)

**Request Body:**
```json
{
  "projectName": "Project Alpha",
  "description": "Implementasi sistem baru",
  "clientId": 1
}
```

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|------------|
| `projectName` | `string` | ✅ | Nama project |
| `description` | `string` | ❌ | Deskripsi project |
| `clientId` | `number` | ✅ | ID client pemilik project |

```bash
curl -X POST http://localhost:8080/api/v1/projects \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{"projectName":"Project Alpha","description":"Implementasi sistem baru","clientId":1}'
```

#### `GET /api/v1/projects` — Ambil Semua Project
```bash
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/v1/projects
```

#### `GET /api/v1/projects/{id}` — Ambil Project by ID
```bash
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/v1/projects/1
```

#### `GET /api/v1/projects/client/{clientId}` — Project per Client
```bash
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/v1/projects/client/1
```

#### `PUT /api/v1/projects/{id}` — Update Project (ADMIN)
```bash
curl -X PUT http://localhost:8080/api/v1/projects/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{"projectName":"Project Alpha v2","description":"Updated","clientId":1}'
```

#### `DELETE /api/v1/projects/{id}` — Hapus Project (ADMIN)
```bash
curl -X DELETE -H "Authorization: Bearer <ADMIN_TOKEN>" \
  http://localhost:8080/api/v1/projects/1
```

---

### 📊 Client Quota API (ADMIN only)

> ⚠️ Semua endpoint Client Quota API memerlukan role **ADMIN**.

#### `POST /api/v1/client-quotas` — Buat Kuota Baru
```bash
curl -X POST http://localhost:8080/api/v1/client-quotas \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"clientId":1,"year":2026,"pmQuota":12,"cmQuota":24}'
```

#### `GET /api/v1/client-quotas` — Ambil Semua Kuota
```bash
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/v1/client-quotas
```

#### `GET /api/v1/client-quotas/client/{clientId}/year/{year}` — Ambil Kuota by Client & Tahun
```bash
curl -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8080/api/v1/client-quotas/client/1/year/2026
```

#### `PUT /api/v1/client-quotas/{id}` — Update Kuota
```bash
curl -X PUT http://localhost:8080/api/v1/client-quotas/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"clientId":1,"year":2026,"pmQuota":18,"cmQuota":30}'
```

#### `DELETE /api/v1/client-quotas/{id}` — Hapus Kuota
```bash
curl -X DELETE -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/v1/client-quotas/1
```

---

### 📋 My Quota API (ADMIN & USER)

> ✅ Endpoint ini dapat diakses oleh role **ADMIN** dan **USER**.
> ClientId diambil otomatis dari data user yang sedang login (JWT token), sehingga USER hanya bisa melihat kuota miliknya sendiri.

#### `GET /api/v1/my-quotas` — Ambil Semua Kuota Client Sendiri

Mengembalikan semua kuota (per tahun) untuk client milik user yang login.

```bash
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/v1/my-quotas
```

**Response — `200 OK`:**
```json
[
  {
    "id": 1,
    "clientId": 3,
    "clientCompanyName": "PT ASTRA DAIHATSU MOTOR",
    "year": 2026,
    "pmQuota": 12,
    "cmQuota": 24,
    "pmUsed": 2,
    "cmUsed": 5
  }
]
```

#### `GET /api/v1/my-quotas/year/{year}` — Ambil Kuota Client Sendiri per Tahun

```bash
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/v1/my-quotas/year/2026
```

**Response — `200 OK`:**
```json
{
  "id": 1,
  "clientId": 3,
  "clientCompanyName": "PT ASTRA DAIHATSU MOTOR",
  "year": 2026,
  "pmQuota": 12,
  "cmQuota": 24,
  "pmUsed": 2,
  "cmUsed": 5
}
```

> 💡 **Perbedaan dengan Client Quota API:**
> - `/api/v1/client-quotas` → ADMIN only, bisa CRUD semua kuota semua client
> - `/api/v1/my-quotas` → ADMIN & USER, hanya bisa **lihat** kuota client sendiri

---

### 🗂 Service Catalog API (ADMIN only)

> ✅ Endpoint untuk mengelola **Service Catalog** — daftar layanan maintenance (PM/CM) yang diberikan untuk setiap client, beserta catatan opsional kontrak/SLA.
> **Aturan penting:** setiap client maksimal **satu** entry catalog. Untuk mengubah, gunakan endpoint update.
> Berbeda dengan **Client Quota** yang menetapkan batas pemakaian per tahun, **Service Catalog** hanya menyatakan layanan apa yang termasuk dalam perjanjian.

#### `POST /api/v1/service-catalogs` — Buat Catalog Baru

**Request Body:**
```json
{
  "clientId": 1,
  "services": ["PM", "CM"],
  "notes": "Layanan PM bulanan + CM on-demand"
}
```

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|------------|
| `clientId` | `number` | ✅ | ID client pemilik catalog |
| `services` | `array[string]` | ✅ | Minimal satu dari `PM`, `CM` |
| `notes` | `string` | ❌ | Catatan opsional (maks 2000 karakter) |

**Response — `201 Created`:**
```json
{
  "id": 1,
  "clientId": 1,
  "clientCompanyName": "PT Contoh Perusahaan",
  "services": ["PM", "CM"],
  "notes": "Layanan PM bulanan + CM on-demand",
  "createdAt": "2026-05-26T10:00:00",
  "updatedAt": "2026-05-26T10:00:00"
}
```

```bash
curl -X POST http://localhost:8080/api/v1/service-catalogs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{"clientId":1,"services":["PM","CM"],"notes":"Layanan PM bulanan + CM on-demand"}'
```

#### `GET /api/v1/service-catalogs` — Ambil Semua Catalog
```bash
curl -H "Authorization: Bearer <ADMIN_TOKEN>" http://localhost:8080/api/v1/service-catalogs
```

#### `GET /api/v1/service-catalogs/{id}` — Ambil Catalog by ID
```bash
curl -H "Authorization: Bearer <ADMIN_TOKEN>" http://localhost:8080/api/v1/service-catalogs/1
```

#### `GET /api/v1/service-catalogs/client/{clientId}` — Ambil Catalog by Client
```bash
curl -H "Authorization: Bearer <ADMIN_TOKEN>" \
  http://localhost:8080/api/v1/service-catalogs/client/1
```

#### `PUT /api/v1/service-catalogs/{id}` — Update Services & Notes

> Hanya `services` dan `notes` yang bisa diubah. `clientId` tidak bisa dipindah ke client lain.

**Request Body:**
```json
{
  "services": ["PM"],
  "notes": "Updated: hanya PM"
}
```

```bash
curl -X PUT http://localhost:8080/api/v1/service-catalogs/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{"services":["PM"],"notes":"Updated: hanya PM"}'
```

#### `DELETE /api/v1/service-catalogs/{id}` — Hapus Catalog
```bash
curl -X DELETE -H "Authorization: Bearer <ADMIN_TOKEN>" \
  http://localhost:8080/api/v1/service-catalogs/1
```

**Error responses:**
- `404 Not Found` — client / catalog tidak ditemukan
- `400 Bad Request` — `services` kosong, atau client sudah punya catalog lain (gunakan `PUT` untuk update)

---

### 🎫 Ticket API (ADMIN & USER)

> ✅ Endpoint Ticket API dapat diakses oleh role **ADMIN** dan **USER**.

#### `POST /api/v1/tickets` — Buat Ticket Baru
```bash
curl -X POST http://localhost:8080/api/v1/tickets \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "title": "Server down",
    "description": "Server produksi tidak bisa diakses",
    "priority": "L1",
    "maintenanceType": "CM",
    "clientId": 1,
    "requesterId": 1
  }'
```

#### `GET /api/v1/tickets` — Ambil Semua Ticket
```bash
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/v1/tickets
```

#### `GET /api/v1/tickets/{id}` — Ambil Ticket by ID
```bash
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/v1/tickets/1
```

#### `GET /api/v1/tickets/number/{ticketNumber}` — Ambil Ticket by Nomor
```bash
curl -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8080/api/v1/tickets/number/TKT-20260522-001
```

#### `PUT /api/v1/tickets/{id}/status` — Update Status Ticket

> Transisi status yang valid:
> - `OPEN` → `IN_PROGRESS`
> - `IN_PROGRESS` → `RESOLVED` / `CLOSED`
> - `RESOLVED` → `CLOSED` / `IN_PROGRESS` (reopen)
> - `CLOSED` → tidak bisa diubah

**Request Body:**
```json
{
  "status": "IN_PROGRESS",
  "changedBy": 1,
  "notes": "Mulai dikerjakan oleh tim teknis"
}
```

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|------------|
| `status` | `string` | ✅ | Status baru (`IN_PROGRESS`, `RESOLVED`, `CLOSED`) |
| `changedBy` | `number` | ✅ | ID user yang mengubah status |
| `notes` | `string` | ❌ | Catatan perubahan (optional) |

```bash
curl -X PUT http://localhost:8080/api/v1/tickets/1/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "status": "IN_PROGRESS",
    "changedBy": 1,
    "notes": "Mulai dikerjakan oleh tim teknis"
  }'
```

#### `GET /api/v1/tickets/{id}/progress` — Riwayat Progress Ticket

```bash
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/v1/tickets/1/progress
```

**Response — `200 OK`:**
```json
[
  {
    "id": 1,
    "ticketId": 1,
    "ticketNumber": "TKT-20260525-001",
    "fromStatus": "OPEN",
    "toStatus": "IN_PROGRESS",
    "changedById": 1,
    "changedByName": "Admin IT",
    "notes": "Mulai dikerjakan oleh tim teknis",
    "changedAt": "2026-05-25T14:30:00"
  },
  {
    "id": 2,
    "ticketId": 1,
    "ticketNumber": "TKT-20260525-001",
    "fromStatus": "IN_PROGRESS",
    "toStatus": "RESOLVED",
    "changedById": 1,
    "changedByName": "Admin IT",
    "notes": "Masalah sudah diperbaiki",
    "changedAt": "2026-05-25T16:00:00"
  }
]
```

#### `GET /api/v1/tickets/export/csv` — Export Tickets ke CSV

> ✅ Endpoint untuk download laporan ticket dalam format CSV. Bisa diakses semua role yang authenticated; **akses data difilter otomatis** sesuai role.

**Query parameters (semua optional):**

| Param | Tipe | Format | Keterangan |
|-------|------|--------|------------|
| `clientId` | `number` | — | Filter ticket untuk satu client |
| `from` | `string` | `yyyy-MM-dd` | Tanggal mulai (inklusif) |
| `to` | `string` | `yyyy-MM-dd` | Tanggal akhir (inklusif sampai 23:59:59) |

**Access control (server-side enforcement):**

| Role | Behaviour |
|------|-----------|
| `ADMIN` | Bisa export semua data; semua filter dihormati |
| `USER` | Otomatis dipaksa ke client miliknya (parameter `clientId` diabaikan) |
| `SUPPORT` / `TECHNICAL_SUPPORT` | Hanya ticket yang di-assign ke mereka; `clientId` dan tanggal tetap berlaku di atas itu |

**Response:** `200 OK`
- `Content-Type: text/csv; charset=UTF-8`
- `Content-Disposition: attachment; filename="tickets-2026-01-01_to_2026-12-31.csv"`

**CSV columns:** `Ticket Number, Title, Description, Status, Priority, Maintenance Type, Client, Project, Requester, Created At`

**Catatan format:**
- File mengandung **UTF-8 BOM** supaya Excel auto-detect encoding (karakter non-ASCII tidak rusak)
- Field di-escape per RFC 4180 (koma/quote/newline di dalam description aman)

**Error:**
- `400 Bad Request` — `from` lebih besar dari `to`

```bash
# Export semua ticket bulan Mei 2026 untuk client 1
curl -H "Authorization: Bearer <TOKEN>" \
  "http://localhost:8080/api/v1/tickets/export/csv?clientId=1&from=2026-05-01&to=2026-05-31" \
  -o tickets.csv
```

---

### 📈 SLA Report API (ADMIN & USER)

> ✅ Laporan performa SLA per client. SLA targets di-hardcode di backend (per priority L1–L4) dan ikut dikembalikan oleh API supaya frontend tidak perlu duplikasi.
> Tracking dilakukan otomatis:
> - `firstResponseAt` ← saat support team (ADMIN/SUPPORT/TECHNICAL_SUPPORT) kirim chat pertama
> - `resolvedAt` ← saat status ticket diubah ke `RESOLVED` pertama kali (tidak overwrite saat reopen)

**SLA Targets default (jam wall-clock):**

| Priority | Response | Resolution |
|----------|----------|------------|
| L1 (Critical) | 1h | 4h |
| L2 (High) | 2h | 8h |
| L3 (Medium) | 4h | 24h |
| L4 (Low) | 8h | 72h |

**State logic per metric:**
- **met** — event terjadi dalam target
- **missed** — event terjadi melewati target, ATAU event belum terjadi dan elapsed time sudah melewati target (in-flight breach)
- **pending** — event belum terjadi tapi masih dalam window target

**Compliance % = met / (met + missed) × 100** (pending tidak dihitung — belum bisa diadjudikasi).

#### `GET /api/v1/sla-report` — SLA Report Lengkap

**Query parameters (semua optional):**

| Param | Tipe | Format | Keterangan |
|-------|------|--------|------------|
| `clientId` | `number` | — | Filter satu client (USER otomatis di-override ke client miliknya) |
| `from` | `string` | `yyyy-MM-dd` | Tanggal mulai (inklusif, by `createdAt`) |
| `to` | `string` | `yyyy-MM-dd` | Tanggal akhir (inklusif sampai 23:59:59) |

**Response — `200 OK`:**
```json
{
  "targets": [
    { "priority": "L1", "responseHours": 1, "resolutionHours": 4 },
    { "priority": "L2", "responseHours": 2, "resolutionHours": 8 },
    { "priority": "L3", "responseHours": 4, "resolutionHours": 24 },
    { "priority": "L4", "responseHours": 8, "resolutionHours": 72 }
  ],
  "clients": [
    {
      "clientId": 1,
      "clientName": "PT BANK NEGARA INDONESIA",
      "totalTickets": 12,
      "response": {
        "met": 8, "missed": 2, "pending": 2,
        "compliancePercent": 80.0,
        "averageHours": 1.75
      },
      "resolution": {
        "met": 6, "missed": 3, "pending": 3,
        "compliancePercent": 66.67,
        "averageHours": 12.4
      },
      "priorityBreakdown": [
        {
          "priority": "L1",
          "totalTickets": 3,
          "response": { "met": 3, "missed": 0, "pending": 0, "compliancePercent": 100.0, "averageHours": 0.5 },
          "resolution": { "met": 2, "missed": 1, "pending": 0, "compliancePercent": 66.67, "averageHours": 4.2 }
        }
      ]
    }
  ],
  "from": "2026-01-01",
  "to": "2026-12-31",
  "generatedAt": "2026-05-29"
}
```

```bash
curl -H "Authorization: Bearer <TOKEN>" \
  "http://localhost:8080/api/v1/sla-report?from=2026-01-01&to=2026-12-31"
```

#### `GET /api/v1/sla-report/targets` — Tabel SLA Target Saja

Endpoint ringan untuk fetch SLA targets tanpa hitung metrik.

```bash
curl -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8080/api/v1/sla-report/targets
```

**Response — `200 OK`:**
```json
[
  { "priority": "L1", "responseHours": 1, "resolutionHours": 4 },
  { "priority": "L2", "responseHours": 2, "resolutionHours": 8 },
  { "priority": "L3", "responseHours": 4, "resolutionHours": 24 },
  { "priority": "L4", "responseHours": 8, "resolutionHours": 72 }
]
```

---

### 🎯 Ticket Assignment API

> ✅ Mendukung **alur eskalasi Tier 1 → Tier 2**:
> - **ADMIN** dapat assign ticket ke `SUPPORT` atau `TECHNICAL_SUPPORT`.
> - **SUPPORT** (Tier 1) dapat **eskalasi** ticket ke `TECHNICAL_SUPPORT` (Tier 2).
> - **TECHNICAL_SUPPORT** dan **SUPPORT** dapat melihat ticket yang di-assign ke mereka via `my-assignments`.
> - SUPPORT **tidak** bisa assign ke sesama SUPPORT — hanya eskalasi ke TECHNICAL_SUPPORT.

#### `POST /api/v1/tickets/{id}/assign` — Assign Support ke Ticket

Assign satu atau lebih support engineer ke ticket. Bisa assign multiple orang sekaligus.

**Request Body:**
```json
{
  "supportUserIds": [3, 5, 7],
  "notes": "Tim teknis untuk handle server issue"
}
```

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|------------|
| `supportUserIds` | `array[number]` | ✅ | List ID user dengan role `SUPPORT` atau `TECHNICAL_SUPPORT` |
| `notes` | `string` | ❌ | Catatan assignment (maks 1000 karakter) |

> **Aturan target:** ADMIN bebas assign ke SUPPORT/TECHNICAL_SUPPORT. SUPPORT hanya boleh assign ke TECHNICAL_SUPPORT (eskalasi).

**Response — `201 Created`:**
```json
[
  {
    "id": 1,
    "ticketId": 1,
    "ticketNumber": "TKT-20260526-001",
    "ticketTitle": "Server down",
    "assignedToId": 3,
    "assignedToName": "Budi Teknisi",
    "assignedToEmail": "budi@support.com",
    "assignedById": 1,
    "assignedByName": "Admin IT",
    "notes": "Tim teknis untuk handle server issue",
    "assignedAt": "2026-05-26T10:00:00",
    "active": true
  }
]
```

**cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/tickets/1/assign \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{"supportUserIds": [3, 5], "notes": "Assign ke tim network"}'
```

#### `POST /api/v1/tickets/{id}/unassign` — Unassign Support dari Ticket

```bash
curl -X POST http://localhost:8080/api/v1/tickets/1/unassign \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{"supportUserIds": [5], "reason": "Pindah ke project lain"}'
```

#### `POST /api/v1/tickets/{id}/reassign` — Reassign Ticket (Ganti Semua Support)

Menghapus semua assignment lama dan assign support baru.

```bash
curl -X POST http://localhost:8080/api/v1/tickets/1/reassign \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{"supportUserIds": [8, 9], "notes": "Reassign ke tim baru"}'
```

#### `GET /api/v1/tickets/{id}/assignments` — Lihat Assignment Ticket

```bash
curl -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8080/api/v1/tickets/1/assignments
```

#### `GET /api/v1/tickets/my-assignments` — Lihat Ticket yang Di-assign ke Saya

Endpoint untuk SUPPORT melihat daftar ticket yang ditugaskan ke mereka.

```bash
curl -H "Authorization: Bearer <SUPPORT_TOKEN>" \
  http://localhost:8080/api/v1/tickets/my-assignments
```

**Response — `200 OK`:**
```json
[
  {
    "id": 1,
    "ticketId": 1,
    "ticketNumber": "TKT-20260526-001",
    "ticketTitle": "Server down",
    "assignedToId": 3,
    "assignedToName": "Budi Teknisi",
    "assignedToEmail": "budi@support.com",
    "assignedById": 1,
    "assignedByName": "Admin IT",
    "notes": "Handle server issue",
    "assignedAt": "2026-05-26T10:00:00",
    "active": true
  }
]
```

---

### 💬 Chat API (WebSocket + REST)

> ✅ Chat API dapat diakses oleh role **ADMIN** dan **USER**.
> Chat dikaitkan dengan **Ticket** — setiap percakapan berada dalam konteks tiket tertentu.
> Mendukung pengiriman **teks**, **file/gambar**, atau **keduanya**.

#### Arsitektur WebSocket

```
┌──────────┐     WebSocket (STOMP)      ┌──────────┐     WebSocket (STOMP)      ┌──────────┐
│   USER   │ ◄════════════════════════► │  SERVER  │ ◄════════════════════════► │  ADMIN   │
└──────────┘                            └──────────┘                            └──────────┘

Kirim:      /app/chat.send              Simpan ke DB              /app/chat.send
Terima:     /topic/chat/{ticketId}       Broadcast                 /topic/chat/{ticketId}
```

#### Cara Menggunakan WebSocket

**1. Connect ke WebSocket**
```javascript
// Menggunakan SockJS + STOMP.js
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect(
    { 'Authorization': 'Bearer <JWT_TOKEN>' },
    () => {
        console.log('Connected!');

        // Subscribe ke chat ticket tertentu
        stompClient.subscribe('/topic/chat/1', (message) => {
            const chatMessage = JSON.parse(message.body);
            console.log('Pesan baru:', chatMessage);
        });
    }
);
```

**2. Kirim Pesan (Teks Saja)**
```javascript
stompClient.send('/app/chat.send', {}, JSON.stringify({
    ticketId: 1,
    content: "Halo, saya butuh bantuan dengan server"
}));
```

**3. Kirim Pesan dengan File Attachment**
```javascript
// Step 1: Upload file via REST terlebih dahulu
const formData = new FormData();
formData.append('ticketId', '1');
formData.append('file', selectedFile);

const uploadRes = await fetch('/api/v1/chat/upload', {
    method: 'POST',
    headers: { 'Authorization': 'Bearer <TOKEN>' },
    body: formData
});
const { id: attachmentId } = await uploadRes.json();

// Step 2: Kirim pesan WebSocket dengan referensi attachment
stompClient.send('/app/chat.send', {}, JSON.stringify({
    ticketId: 1,
    content: "Ini screenshot errornya",
    attachmentIds: [attachmentId]
}));
```

**4. Format Pesan yang Diterima**
```json
{
  "id": 1,
  "ticketId": 1,
  "ticketNumber": "TKT-20260525-001",
  "senderId": 2,
  "senderName": "John Doe",
  "senderRole": "USER",
  "content": "Ini screenshot errornya",
  "sentAt": "2026-05-25T10:30:00",
  "attachments": [
    {
      "id": 1,
      "fileName": "screenshot.png",
      "fileType": "image/png",
      "fileSize": 234567,
      "downloadUrl": "/api/v1/chat/attachments/1/download"
    }
  ]
}
```

> 💡 Pesan bisa berisi hanya teks, hanya attachment, atau keduanya.

#### REST Endpoints (Histori Chat)

#### `GET /api/v1/chat/{ticketId}` — Ambil Histori Chat by Ticket ID

```bash
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/v1/chat/1
```

**Response — `200 OK`:**
```json
[
  {
    "id": 1,
    "ticketId": 1,
    "ticketNumber": "TKT-20260525-001",
    "senderId": 2,
    "senderName": "John Doe",
    "senderRole": "USER",
    "content": "Halo, server produksi down",
    "sentAt": "2026-05-25T10:30:00",
    "attachments": []
  },
  {
    "id": 2,
    "ticketId": 1,
    "ticketNumber": "TKT-20260525-001",
    "senderId": 1,
    "senderName": "Admin IT",
    "senderRole": "ADMIN",
    "content": "Baik, sedang kami cek. Mohon tunggu.",
    "sentAt": "2026-05-25T10:31:00",
    "attachments": []
  }
]
```

#### `GET /api/v1/chat/ticket/{ticketNumber}` — Ambil Histori Chat by Nomor Ticket

```bash
curl -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8080/api/v1/chat/ticket/TKT-20260525-001
```

---

### 📎 Chat Attachment API (ADMIN & USER)

> ✅ Endpoint untuk upload dan download file di dalam chat.
> File diupload terlebih dahulu via REST, kemudian di-referensikan saat mengirim pesan WebSocket.

#### `POST /api/v1/chat/upload` — Upload File untuk Chat

Upload file yang akan dikirim di chat. Response berisi `id` (attachmentId) yang harus disertakan di pesan WebSocket.

```bash
curl -X POST http://localhost:8080/api/v1/chat/upload \
  -H "Authorization: Bearer <TOKEN>" \
  -F "ticketId=1" \
  -F "file=@/path/to/screenshot.png"
```

**Response — `200 OK`:**
```json
{
  "id": 1,
  "fileName": "screenshot.png",
  "fileType": "image/png",
  "fileSize": 234567,
  "downloadUrl": "/api/v1/chat/attachments/1/download"
}
```

| Parameter | Tipe | Wajib | Keterangan |
|-----------|------|-------|------------|
| `ticketId` | `number` | ✅ | ID ticket terkait |
| `file` | `file` | ✅ | File yang akan diupload |

#### `GET /api/v1/chat/attachments/{id}/download` — Download File Chat

Download file lampiran chat. Access control: user harus memiliki akses ke ticket terkait.

```bash
curl -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8080/api/v1/chat/attachments/1/download \
  -o screenshot.png
```

#### Alur Lengkap Kirim File di Chat

```
1. Upload file    →  POST /api/v1/chat/upload  →  dapat attachmentId
2. Kirim pesan    →  WebSocket /app/chat.send   →  { ticketId, content, attachmentIds: [id] }
3. Server proses  →  Link attachment ke message  →  Broadcast ke /topic/chat/{ticketId}
4. Download file  →  GET /api/v1/chat/attachments/{id}/download
```

---

### 🔔 Notification API

> ✅ Dapat diakses oleh semua role yang sudah login. Digunakan untuk menampilkan badge notifikasi di frontend tanpa polling — cukup panggil ulang saat menerima event WebSocket.

#### `GET /api/v1/notifications/unread-count` — Jumlah Notifikasi Belum Dibaca

Mengembalikan jumlah ticket baru yang belum dibuka dan pesan chat yang belum dibaca, dihitung berdasarkan hak akses user yang login.

```bash
curl -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8082/api/v1/notifications/unread-count
```

**Response — `200 OK`:**
```json
{
  "unreadTickets": 3,
  "unreadMessages": 7,
  "total": 10
}
```

| Field | Keterangan |
|-------|------------|
| `unreadTickets` | Ticket baru yang belum pernah dibuka user |
| `unreadMessages` | Pesan chat baru di ticket yang sudah pernah dibuka |
| `total` | `unreadTickets + unreadMessages` |

#### `POST /api/v1/tickets/{ticketId}/read` — Tandai Ticket Sudah Dibaca

Dipanggil saat user membuka halaman detail ticket. Me-reset watermark `last_read_at` di database sehingga badge notifikasi untuk ticket tersebut hilang.

```bash
curl -X POST -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8082/api/v1/tickets/1/read
```

**Response — `200 OK`** (body kosong)

> 💡 Panggil endpoint ini setiap kali halaman detail ticket dibuka, bukan hanya saat pertama kali. Ini memastikan pesan-pesan chat baru juga tertandai terbaca.

---

### ⏱ Ticket Worklog API

> ✅ Worklog adalah **live timer** untuk mencatat waktu pengerjaan ticket secara nyata.
> - **Start/Stop timer**: ADMIN, SUPPORT, TECHNICAL_SUPPORT
> - **Lihat worklog**: semua role yang punya akses ke ticket
>
> Jika `targetUserId` disertakan saat start, worklog dicatat atas nama user tersebut (misal: SUPPORT memulai timer untuk TECHNICAL_SUPPORT).

**Base path:** `/api/v1/tickets/{ticketId}/worklogs`

#### `POST /api/v1/tickets/{ticketId}/worklogs` — Mulai Worklog

**Request Body (optional):**
```json
{
  "targetUserId": 5,
  "notes": "Mulai investigasi koneksi database"
}
```

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|------------|
| `targetUserId` | `number` | ❌ | ID user yang bekerja (default: user yang login) |
| `notes` | `string` | ❌ | Catatan awal worklog |

**Response — `201 Created`:**
```json
{
  "id": 1,
  "ticketId": 1,
  "ticketNumber": "TKT-20260630-001",
  "userId": 5,
  "userName": "Budi Teknisi",
  "startedAt": "2026-06-30T10:00:00",
  "stoppedAt": null,
  "loggedDurationSeconds": null,
  "notes": "Mulai investigasi koneksi database"
}
```

```bash
curl -X POST http://localhost:8082/api/v1/tickets/1/worklogs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"targetUserId": 5, "notes": "Mulai investigasi"}'
```

#### `GET /api/v1/tickets/{ticketId}/worklogs` — Ambil Semua Worklog Ticket

```bash
curl -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8082/api/v1/tickets/1/worklogs
```

**Response — `200 OK`:**
```json
[
  {
    "id": 1,
    "ticketId": 1,
    "ticketNumber": "TKT-20260630-001",
    "userId": 5,
    "userName": "Budi Teknisi",
    "startedAt": "2026-06-30T10:00:00",
    "stoppedAt": "2026-06-30T11:30:00",
    "loggedDurationSeconds": 5400,
    "notes": "Investigasi selesai, masalah ditemukan di konfigurasi firewall"
  }
]
```

#### `PUT /api/v1/tickets/{ticketId}/worklogs/{worklogId}/stop` — Stop Worklog

**Request Body:**
```json
{
  "stoppedAt": "2026-06-30T11:30:00",
  "loggedDurationSeconds": 5400,
  "notes": "Investigasi selesai, masalah ditemukan di konfigurasi firewall"
}
```

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|------------|
| `stoppedAt` | `string` (ISO 8601) | ✅ | Waktu berhenti |
| `loggedDurationSeconds` | `number` | ✅ | Durasi dalam detik (dari client timer) |
| `notes` | `string` | ❌ | Catatan akhir worklog |

```bash
curl -X PUT http://localhost:8082/api/v1/tickets/1/worklogs/1/stop \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "stoppedAt": "2026-06-30T11:30:00",
    "loggedDurationSeconds": 5400,
    "notes": "Investigasi selesai"
  }'
```

---

## 📖 Enum Reference

### Role
| Nilai | Tier | Keterangan |
|-------|------|------------|
| `ADMIN` | — | Full access ke semua fitur, dapat assign ticket ke siapa saja |
| `SUPPORT` | Tier 1 | Customer-facing support, auto-assigned ke ticket dari client-nya, bisa eskalasi ke TECHNICAL_SUPPORT |
| `TECHNICAL_SUPPORT` | Tier 2/3 | Technical engineer, hanya menerima ticket eskalasi dan mengerjakan ticket yang di-assign |
| `USER` | — | Hanya bisa create & view ticket milik client-nya |

### Priority
| Nilai | Keterangan |
|-------|------------|
| `L1` | Urgent / Critical |
| `L2` | High |
| `L3` | Medium |
| `L4` | Low |

### MaintenanceType
| Nilai | Keterangan |
|-------|------------|
| `PM` | Preventive Maintenance |
| `CM` | Corrective Maintenance |

### TicketStatus
| Nilai | Keterangan |
|-------|------------|
| `OPEN` | Ticket baru dibuat |
| `IN_PROGRESS` | Sedang dikerjakan |
| `RESOLVED` | Masalah diselesaikan |
| `CLOSED` | Ticket ditutup |

---

## ❌ Error Handling

### Error Response Format
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid email or password",
  "timestamp": "2026-05-22T10:30:00",
  "validationErrors": null
}
```

### Daftar Error

| Status | Error | Kondisi |
|--------|-------|---------|
| `400` | Validation Failed | Field required tidak diisi / format invalid |
| `401` | Unauthorized | Token tidak ada / expired / invalid credentials |
| `403` | Forbidden | Role tidak memiliki akses ke endpoint / security policy violation |
| `404` | Not Found | Resource tidak ditemukan |
| `409` | Conflict | Data duplikat (email, kuota) / password policy violation |
| `413` | Payload Too Large | File upload melebihi batas maksimum (10MB) |
| `422` | Quota Exceeded | Kuota maintenance habis |
| `429` | Too Many Requests | Rate limit terlampaui (tunggu 60 detik) |
| `500` | Internal Server Error | Error tak terduga (detail tidak di-expose ke client) |

---

## 📂 Struktur Project

```
ticketing-backend/
├── pom.xml
├── README.md
└── src/main/java/com/itsm/ticketing/
    ├── TicketingBackendApplication.java
    ├── config/
    │   ├── CorsConfig.java                # CORS strict configuration
    │   ├── SecurityConfig.java            # Spring Security + RBAC + filters
    │   ├── WebSocketConfig.java           # WebSocket STOMP configuration
    │   └── ... (other configs)
    ├── controller/
    │   ├── AuthController.java            # Login & Register
    │   ├── AttachmentController.java      # Ticket attachment download
    │   ├── ChatController.java            # WebSocket chat + REST history + file upload/download
    │   ├── ClientController.java          # CRUD client (ADMIN)
    │   ├── ClientQuotaController.java     # CRUD kuota (ADMIN)
    │   ├── MyQuotaController.java         # User's own quota (ADMIN+USER)
    │   ├── NotificationController.java    # Unread count & mark-as-read
    │   ├── ProjectController.java         # CRUD project (ADMIN+USER)
    │   ├── ServiceCatalogController.java  # CRUD service catalog per client (ADMIN)
    │   ├── SlaReportController.java       # SLA report (ADMIN+USER)
    │   ├── TicketAssignmentController.java# Assign/unassign/reassign ticket
    │   ├── TicketController.java          # CRUD ticket + export CSV
    │   ├── TicketWorklogController.java   # Live worklog timer (ADMIN+SUPPORT+TECHNICAL_SUPPORT)
    │   └── UserController.java            # CRUD user (ADMIN)
    ├── dto/
    │   ├── ApiErrorResponse.java
    │   ├── AuthResponse.java              # JWT token response (includes clientId)
    │   ├── ChatAttachmentInfo.java        # Attachment info in chat response
    │   ├── ChatMessageRequest.java        # Chat send request (+ attachmentIds)
    │   ├── ChatMessageResponse.java       # Chat message response (+ attachments)
    │   ├── ChatUploadResponse.java        # Chat file upload response
    │   ├── LoginRequest.java              # Login request
    │   ├── RegisterRequest.java           # Registration request
    │   └── ... (other DTOs)
    ├── entity/
    │   ├── ChatAttachment.java            # Chat file attachment entity
    │   ├── ChatMessage.java               # Chat message entity (+ attachments relation)
    │   ├── Role.java                      # ADMIN, SUPPORT, TECHNICAL_SUPPORT, USER
    │   ├── User.java                      # Implements UserDetails
    │   └── ... (other entities)
    ├── exception/
    │   └── GlobalExceptionHandler.java    # Includes 401, 403 handlers
    ├── security/
    │   ├── FileValidationUtil.java           # File upload validation (CWE-434)
    │   ├── InputSanitizer.java               # Input validation & XSS prevention
    │   ├── JwtAuthenticationEntryPoint.java  # 401 handler
    │   ├── JwtAuthenticationFilter.java      # JWT request filter
    │   ├── JwtUtils.java                     # JWT token utilities
    │   ├── RateLimitingFilter.java           # Rate limiting (CWE-307)
    │   ├── SecurityAuditLogger.java          # Security event audit logging
    │   ├── SecurityHeadersFilter.java        # OWASP security headers
    │   └── WebSocketAuthInterceptor.java     # JWT auth for WebSocket
    ├── repository/
    │   ├── ChatAttachmentRepository.java  # Chat attachment queries
    │   ├── ChatMessageRepository.java     # Chat message queries
    │   └── ... (other repositories)
    └── service/
        ├── AuthService.java               # Register & Login logic
        ├── ChatService.java               # Chat send, history, file upload/download
        ├── UserService.java               # + UserDetailsService
        └── ... (other services)
```

---

## 🔔 Real-time Ticket Updates (WebSocket)

Selain chat, frontend dapat subscribe ke topic WebSocket berikut untuk mendapatkan notifikasi **tanpa polling** saat data ticket berubah.

### Topic Layout

| Topic | Trigger |
|-------|---------|
| `/topic/tickets/new` | Ticket baru dibuat |
| `/topic/tickets/{id}/status` | Status ticket berubah |
| `/topic/tickets/{id}/assigned` | Assignment ticket berubah (assign/unassign/reassign) |

> **Broadcast dilakukan setelah DB commit** (`@TransactionalEventListener(phase = AFTER_COMMIT)`). Jadi ketika frontend menerima notifikasi dan langsung refetch, data sudah pasti ada di database.

### Payload

Semua topic mengirimkan `TicketResponse` (format sama dengan REST API):

```json
{
  "id": 1,
  "ticketNumber": "TKT-20260529-001",
  "title": "Server down",
  "status": "IN_PROGRESS",
  "priority": "L1",
  "maintenanceType": "CM",
  "clientId": 1,
  "clientCompanyName": "PT BANK NEGARA INDONESIA",
  "createdAt": "2026-05-29T10:00:00"
}
```

> Untuk `ASSIGNED` topic, payload hanya berisi field dasar (id, ticketNumber, status, client) — cukup untuk trigger re-fetch assignments dari REST.

### Cara Subscribe di Frontend

```javascript
// 1. Connect (sama seperti chat, JWT di header STOMP)
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({ 'Authorization': 'Bearer <TOKEN>' }, () => {

    // 2a. Subscribe ticket baru → refresh list
    stompClient.subscribe('/topic/tickets/new', (msg) => {
        const ticket = JSON.parse(msg.body);
        // tambahkan ticket ke list, atau trigger re-fetch
        addTicketToList(ticket);
    });

    // 2b. Subscribe perubahan status ticket tertentu
    stompClient.subscribe(`/topic/tickets/${ticketId}/status`, (msg) => {
        const ticket = JSON.parse(msg.body);
        updateTicketStatus(ticket);
    });

    // 2c. Subscribe perubahan assignment ticket tertentu
    stompClient.subscribe(`/topic/tickets/${ticketId}/assigned`, (msg) => {
        fetchTicketAssignments(ticketId); // refetch assignments
    });
});
```

### Pattern yang Direkomendasikan

Karena `/topic/tickets/new` broadcast ke semua subscriber (tanpa filter role/client), filter di **frontend** sesuai role:

```javascript
stompClient.subscribe('/topic/tickets/new', (msg) => {
    const ticket = JSON.parse(msg.body);

    // ADMIN: tampilkan semua
    if (userRole === 'ADMIN') {
        addTicketToList(ticket);
        return;
    }

    // USER: hanya ticket dari client-nya
    if (userRole === 'USER' && ticket.clientId === myClientId) {
        addTicketToList(ticket);
        return;
    }

    // SUPPORT/TECHNICAL_SUPPORT: tunggu notif assignment,
    // jangan langsung tampilkan saat ticket dibuat
});
```

---

## 🔄 Alur Penggunaan

```
1.  Register Admin  →  POST /api/v1/auth/register  (role: ADMIN)
2.  Login           →  POST /api/v1/auth/login      (dapatkan token)
3.  Buat Client     →  POST /api/v1/clients         (token ADMIN)
4.  Buat Kuota      →  POST /api/v1/client-quotas   (token ADMIN)
5.  Register User   →  POST /api/v1/auth/register   (role: USER, clientId)
6.  Login User      →  POST /api/v1/auth/login      (dapatkan token + clientId)
7.  Cek Kuota       →  GET /api/v1/my-quotas              (token USER → kuota sendiri)
8.  Buat Ticket     →  POST /api/v1/tickets               (token USER/ADMIN)
9.  Connect Chat    →  WebSocket ws://localhost:8082/ws    (STOMP + JWT)
10. Kirim Pesan     →  STOMP /app/chat.send               (real-time)
11. Upload File     →  POST /api/v1/chat/upload           (REST, dapat attachmentId)
12. Kirim + File    →  STOMP /app/chat.send               ({ attachmentIds: [id] })
13. Histori Chat    →  GET /api/v1/chat/{ticketId}        (REST, termasuk attachments)
14. Cek Notifikasi  →  GET /api/v1/notifications/unread-count (setelah terima WS event)
15. Buka Ticket     →  POST /api/v1/tickets/{id}/read     (reset badge notifikasi)
16. Start Worklog   →  POST /api/v1/tickets/{id}/worklogs (mulai timer)
17. Stop Worklog    →  PUT /api/v1/tickets/{id}/worklogs/{wId}/stop
```

---

## 📜 License

This project is for internal/educational purposes.

---

## 🛡 Security Hardening

Sistem ini telah menerapkan security hardening komprehensif berdasarkan standar keamanan internasional dan nasional:

### Framework & Standar yang Diterapkan

| Framework | Cakupan |
|-----------|---------|
| **OWASP Top 10** | A01 (Broken Access Control), A02 (Cryptographic Failures), A03 (Injection), A04 (Insecure Design), A05 (Security Misconfiguration), A07 (Identification & Auth Failures), A09 (Security Logging & Monitoring) |
| **NIST SP 800-53** | AC-3 (Access Enforcement), AU-2/AU-3 (Audit Events), CM-7 (Least Functionality), IA-11 (Re-authentication), SC-8 (Transmission Confidentiality), SI-3 (Malicious Code Protection), SI-11 (Error Handling) |
| **NIST SP 800-63B** | Password policy, session management (2 jam expiration) |
| **CWE/SANS** | CWE-22, CWE-79, CWE-200, CWE-204, CWE-209, CWE-307, CWE-434, CWE-521, CWE-693, CWE-778, CWE-942, CWE-1021 |
| **BSSN** | Standar Keamanan Aplikasi — Validasi input, audit logging, enkripsi, pembatasan akses |

### Implementasi Security

#### 1. Security Headers (OWASP Secure Headers)
- `X-Content-Type-Options: nosniff` — Mencegah MIME type sniffing
- `X-Frame-Options: DENY` — Mencegah clickjacking
- `Strict-Transport-Security` — Enforce HTTPS
- `Content-Security-Policy` — Mencegah XSS
- `Referrer-Policy` — Mencegah information leakage
- `Permissions-Policy` — Membatasi fitur browser
- `Cache-Control: no-store` — Mencegah caching data sensitif

#### 2. Rate Limiting (CWE-307)
- **Auth endpoints** (`/api/v1/auth/**`): 10 request/menit per IP
- **API endpoints** (`/api/**`): 100 request/menit per IP
- Response `429 Too Many Requests` dengan header `Retry-After`

#### 3. Password Policy (NIST SP 800-63B)
- Minimum 8 karakter, maksimum 128 karakter
- Wajib mengandung: huruf besar, huruf kecil, angka, dan karakter spesial
- BCrypt dengan strength 12 rounds

#### 4. File Upload Security (CWE-434)
- Whitelist MIME type (image, document, text, archive)
- Whitelist file extension
- Blacklist ekstensi berbahaya (exe, bat, sh, php, dll)
- Deteksi double extension attack
- Maksimum file size: 10MB
- Filename sanitization

#### 5. Path Traversal Protection (CWE-22)
- Validasi resolved path tetap dalam upload directory
- Sanitasi filename (hapus `..`, `/`, `\`)
- UUID prefix untuk stored filename

#### 6. Input Validation & Sanitization (OWASP)
- Size constraints pada semua DTO fields
- XSS pattern detection
- Email format validation
- Phone number format validation
- HTML entity encoding

#### 7. Error Handling Hardening (CWE-209)
- Tidak pernah expose stack trace ke client
- Generic error message untuk unexpected errors
- Internal logging untuk debugging
- Specific handlers untuk security exceptions

#### 8. CORS Configuration (CWE-942)
- Strict origin whitelist (configurable via properties)
- Specific allowed methods dan headers
- Preflight cache 1 jam

#### 9. Security Audit Logging (NIST AU-2)
- Log semua authentication success/failure
- Log user registration events
- Log access denied events
- Log file upload/rejection events
- Log suspicious activity (XSS attempts, path traversal)
- Format: `SECURITY_AUDIT: EVENT_TYPE | details`

#### 10. Session Management
- JWT token expiration: **2 jam**
- Stateless session (no server-side session)
- Token invalidation on expiry

### Konfigurasi Security

```properties
# CORS (tambahkan domain frontend production)
security.cors.allowed-origins=http://localhost:3000,http://localhost:5173

# JWT Session (2 jam)
jwt.expiration=7200000

# Actuator (hanya health endpoint)
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=never

# Error disclosure prevention
server.error.include-stacktrace=never
server.error.include-message=never
```

### Catatan untuk Frontend Developer

Setelah security hardening, perhatikan hal berikut:

1. **Rate Limiting** — Handle response `429 Too Many Requests` dengan retry/backoff
2. **Password Policy** — Tampilkan requirement saat register (min 8 char, uppercase, lowercase, digit, special char)
3. **CORS** — Pastikan domain frontend terdaftar di `security.cors.allowed-origins`
4. **File Upload** — Hanya file dengan extension yang di-whitelist yang diterima (jpg, png, pdf, docx, xlsx, dll)
5. **Token Expiry** — Token berlaku 2 jam, implementasikan refresh mechanism di client
