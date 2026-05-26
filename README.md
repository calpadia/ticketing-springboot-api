# 🎫 B2B ITSM Ticketing System — Backend API

Sistem ticketing untuk manajemen IT Service Management (ITSM) berbasis B2B. Backend dibangun dengan Spring Boot dan PostgreSQL, mendukung **JWT Authentication**, **Role-Based Access Control (RBAC)** dengan role ADMIN dan USER, serta **Real-time Chat** via WebSocket.

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
  - [Client Quota API](#-client-quota-api)
  - [My Quota API](#-my-quota-api-admin--user)
  - [Ticket API](#-ticket-api)
  - [Chat API (WebSocket + REST)](#-chat-api-websocket--rest)
  - [Chat Attachment API](#-chat-attachment-api-admin--user)
- [Enum Reference](#-enum-reference)
- [Error Handling](#-error-handling)

---

## 🛠 Tech Stack

| Teknologi | Versi |
|-----------|-------|
| Java | 17+ |
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

- **JDK 17** atau lebih baru
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

Aplikasi akan berjalan di `http://localhost:8080`

---

## ⚙ Konfigurasi

Edit file `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/ticketing_db
spring.datasource.username=postgres
spring.datasource.password=your_password

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.show-sql=true

# Server
server.port=8080

# JWT Configuration
jwt.secret=<your-base64-encoded-256-bit-secret-key>
jwt.expiration=7200000
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

| Role | Deskripsi |
|------|-----------|
| `ADMIN` | Full access ke semua fitur sistem, dapat assign ticket ke support |
| `SUPPORT` | Technical support staff, dapat di-assign ke ticket dan mengerjakannya |
| `USER` | Akses terbatas — hanya bisa membuat dan melihat ticket |

### Access Control Matrix

| Endpoint | ADMIN | SUPPORT | USER | Public |
|----------|-------|---------|------|--------|
| `POST /api/v1/auth/register` | — | — | — | ✅ |
| `POST /api/v1/auth/login` | — | — | — | ✅ |
| `GET/POST/PUT/DELETE /api/v1/users/**` | ✅ | ❌ | ❌ | ❌ |
| `GET/POST/PUT/DELETE /api/v1/clients/**` | ✅ | ❌ | ❌ | ❌ |
| `GET/POST/PUT/DELETE /api/v1/client-quotas/**` | ✅ | ❌ | ❌ | ❌ |
| `GET /api/v1/my-quotas/**` | ✅ | ❌ | ✅ | ❌ |
| `POST /api/v1/tickets` | ✅ | ❌ | ✅ | ❌ |
| `GET /api/v1/tickets/**` | ✅ | ✅ (assigned) | ✅ (own client) | ❌ |
| `POST /api/v1/tickets/{id}/assign` | ✅ | ❌ | ❌ | ❌ |
| `POST /api/v1/tickets/{id}/unassign` | ✅ | ❌ | ❌ | ❌ |
| `POST /api/v1/tickets/{id}/reassign` | ✅ | ❌ | ❌ | ❌ |
| `GET /api/v1/tickets/{id}/assignments` | ✅ | ✅ | ❌ | ❌ |
| `GET /api/v1/tickets/my-assignments` | ✅ | ✅ | ❌ | ❌ |
| `GET/POST /api/v1/chat/**` | ✅ | ✅ | ✅ | ❌ |
| `WebSocket /ws` (handshake) | — | — | — | ✅ |
| `STOMP /app/chat.send` | ✅ | ✅ | ✅ | ❌ |

---

## 📡 API Endpoints

**Base URL:** `http://localhost:8080`

### Ringkasan Seluruh API

| Method | Endpoint | Deskripsi | Auth |
|--------|----------|-----------|------|
| **AUTH** | | | |
| `POST` | `/api/v1/auth/register` | Registrasi user baru | Public |
| `POST` | `/api/v1/auth/login` | Login & dapatkan token | Public |
| **USER** | | | |
| `POST` | `/api/v1/users` | Buat user baru | ADMIN |
| `GET` | `/api/v1/users` | Ambil semua user | ADMIN |
| `GET` | `/api/v1/users/{id}` | Ambil user by ID | ADMIN |
| `PUT` | `/api/v1/users/{id}` | Update user | ADMIN |
| `DELETE` | `/api/v1/users/{id}` | Hapus user | ADMIN |
| **CLIENT** | | | |
| `POST` | `/api/v1/clients` | Buat client baru | ADMIN |
| `GET` | `/api/v1/clients` | Ambil semua client | ADMIN |
| `GET` | `/api/v1/clients/{id}` | Ambil client by ID | ADMIN |
| `PUT` | `/api/v1/clients/{id}` | Update client | ADMIN |
| `DELETE` | `/api/v1/clients/{id}` | Hapus client | ADMIN |
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
| **TICKET** | | | |
| `POST` | `/api/v1/tickets` | Buat ticket baru | ADMIN, USER |
| `GET` | `/api/v1/tickets` | Ambil semua ticket | ADMIN, USER |
| `GET` | `/api/v1/tickets/{id}` | Ambil ticket by ID | ADMIN, USER |
| `GET` | `/api/v1/tickets/number/{no}` | Ambil ticket by nomor | ADMIN, USER |
| `PUT` | `/api/v1/tickets/{id}/status` | Update status ticket | ADMIN, USER |
| `GET` | `/api/v1/tickets/{id}/progress` | Riwayat progress ticket | ADMIN, USER |
| **CHAT** | | | |
| `WS` | `/ws` | WebSocket handshake (SockJS) | Public |
| `STOMP` | `/app/chat.send` | Kirim pesan chat real-time | ADMIN, USER |
| `STOMP` | `/topic/chat/{ticketId}` | Subscribe pesan chat | ADMIN, USER |
| `GET` | `/api/v1/chat/{ticketId}` | Histori chat by ticket ID | ADMIN, USER |
| `GET` | `/api/v1/chat/ticket/{no}` | Histori chat by ticket number | ADMIN, USER |
| **CHAT ATTACHMENT** | | | |
| `POST` | `/api/v1/chat/upload` | Upload file untuk chat | ADMIN, USER |
| `GET` | `/api/v1/chat/attachments/{id}/download` | Download file chat | ADMIN, USER |
| **TICKET ASSIGNMENT** | | | |
| `POST` | `/api/v1/tickets/{id}/assign` | Assign support ke ticket | ADMIN |
| `POST` | `/api/v1/tickets/{id}/unassign` | Unassign support dari ticket | ADMIN |
| `POST` | `/api/v1/tickets/{id}/reassign` | Reassign ticket ke support baru | ADMIN |
| `GET` | `/api/v1/tickets/{id}/assignments` | Lihat assignment ticket | ADMIN, SUPPORT |
| `GET` | `/api/v1/tickets/my-assignments` | Lihat ticket yang di-assign ke saya | ADMIN, SUPPORT |

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
| `role` | `string` | ❌ | `ADMIN` atau `USER` (default: `USER`) |

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

---

### 🎯 Ticket Assignment API (ADMIN only for assign/unassign)

> ✅ Fitur assignment memungkinkan ADMIN untuk menugaskan satu atau lebih **SUPPORT** engineer ke sebuah ticket.
> SUPPORT engineer hanya bisa melihat ticket yang di-assign ke mereka.

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
| `supportUserIds` | `array[number]` | ✅ | List ID user dengan role SUPPORT |
| `notes` | `string` | ❌ | Catatan assignment (maks 1000 karakter) |

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

## 📖 Enum Reference

### Role
| Nilai | Keterangan |
|-------|------------|
| `ADMIN` | Full access ke semua fitur, dapat assign ticket |
| `SUPPORT` | Technical support, dapat di-assign ke ticket dan mengerjakannya |
| `USER` | Hanya bisa create & view ticket |

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
    │   ├── TicketController.java          # CRUD ticket (ADMIN+USER)
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
    │   ├── Role.java                      # ADMIN, USER
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

## 🔄 Alur Penggunaan

```
1.  Register Admin  →  POST /api/v1/auth/register  (role: ADMIN)
2.  Login           →  POST /api/v1/auth/login      (dapatkan token)
3.  Buat Client     →  POST /api/v1/clients         (token ADMIN)
4.  Buat Kuota      →  POST /api/v1/client-quotas   (token ADMIN)
5.  Register User   →  POST /api/v1/auth/register   (role: USER, clientId)
6.  Login User      →  POST /api/v1/auth/login      (dapatkan token + clientId)
7.  Cek Kuota       →  GET /api/v1/my-quotas         (token USER → kuota sendiri)
8.  Buat Ticket     →  POST /api/v1/tickets          (token USER/ADMIN)
9.  Connect Chat    →  WebSocket ws://localhost:8080/ws (STOMP + JWT)
10. Kirim Pesan     →  STOMP /app/chat.send           (real-time)
11. Upload File     →  POST /api/v1/chat/upload       (REST, dapat attachmentId)
12. Kirim + File    →  STOMP /app/chat.send           ({ attachmentIds: [id] })
13. Histori Chat    →  GET /api/v1/chat/{ticketId}    (REST, termasuk attachments)
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
