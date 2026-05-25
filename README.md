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
  - [Ticket API](#-ticket-api)
  - [Chat API (WebSocket + REST)](#-chat-api-websocket--rest)
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
jwt.expiration=86400000
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

Token berlaku **24 jam** (dapat dikonfigurasi via `jwt.expiration`).

---

## 🛡 Role-Based Access Control

### Role yang Tersedia

| Role | Deskripsi |
|------|-----------|
| `ADMIN` | Full access ke semua fitur sistem |
| `USER` | Akses terbatas — hanya bisa membuat dan melihat ticket |

### Access Control Matrix

| Endpoint | ADMIN | USER | Public |
|----------|-------|------|--------|
| `POST /api/v1/auth/register` | — | — | ✅ |
| `POST /api/v1/auth/login` | — | — | ✅ |
| `GET/POST/PUT/DELETE /api/v1/users/**` | ✅ | ❌ | ❌ |
| `GET/POST/PUT/DELETE /api/v1/clients/**` | ✅ | ❌ | ❌ |
| `GET/POST/PUT/DELETE /api/v1/client-quotas/**` | ✅ | ❌ | ❌ |
| `POST /api/v1/tickets` | ✅ | ✅ | ❌ |
| `GET /api/v1/tickets/**` | ✅ | ✅ | ❌ |
| `GET /api/v1/chat/**` | ✅ | ✅ | ❌ |
| `WebSocket /ws` (handshake) | — | — | ✅ |
| `STOMP /app/chat.send` | ✅ | ✅ | ❌ |

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
| **TICKET** | | | |
| `POST` | `/api/v1/tickets` | Buat ticket baru | ADMIN, USER |
| `GET` | `/api/v1/tickets` | Ambil semua ticket | ADMIN, USER |
| `GET` | `/api/v1/tickets/{id}` | Ambil ticket by ID | ADMIN, USER |
| `GET` | `/api/v1/tickets/number/{no}` | Ambil ticket by nomor | ADMIN, USER |
| **CHAT** | | | |
| `WS` | `/ws` | WebSocket handshake (SockJS) | Public |
| `STOMP` | `/app/chat.send` | Kirim pesan chat real-time | ADMIN, USER |
| `STOMP` | `/topic/chat/{ticketId}` | Subscribe pesan chat | ADMIN, USER |
| `GET` | `/api/v1/chat/{ticketId}` | Histori chat by ticket ID | ADMIN, USER |
| `GET` | `/api/v1/chat/ticket/{no}` | Histori chat by ticket number | ADMIN, USER |

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
  -d '{"companyName": "PT Contoh Perusahaan"}'
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
  -d '{"companyName": "PT Updated"}'
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

---

### 💬 Chat API (WebSocket + REST)

> ✅ Chat API dapat diakses oleh role **ADMIN** dan **USER**.
> Chat dikaitkan dengan **Ticket** — setiap percakapan berada dalam konteks tiket tertentu.

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

**2. Kirim Pesan**
```javascript
stompClient.send('/app/chat.send', {}, JSON.stringify({
    ticketId: 1,
    content: "Halo, saya butuh bantuan dengan server"
}));
```

**3. Format Pesan yang Diterima**
```json
{
  "id": 1,
  "ticketId": 1,
  "ticketNumber": "TKT-20260525-001",
  "senderId": 2,
  "senderName": "John Doe",
  "senderRole": "USER",
  "content": "Halo, saya butuh bantuan dengan server",
  "sentAt": "2026-05-25T10:30:00"
}
```

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
    "sentAt": "2026-05-25T10:30:00"
  },
  {
    "id": 2,
    "ticketId": 1,
    "ticketNumber": "TKT-20260525-001",
    "senderId": 1,
    "senderName": "Admin IT",
    "senderRole": "ADMIN",
    "content": "Baik, sedang kami cek. Mohon tunggu.",
    "sentAt": "2026-05-25T10:31:00"
  }
]
```

#### `GET /api/v1/chat/ticket/{ticketNumber}` — Ambil Histori Chat by Nomor Ticket

```bash
curl -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8080/api/v1/chat/ticket/TKT-20260525-001
```

---

## 📖 Enum Reference

### Role
| Nilai | Keterangan |
|-------|------------|
| `ADMIN` | Full access ke semua fitur |
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
| `403` | Forbidden | Role tidak memiliki akses ke endpoint |
| `404` | Not Found | Resource tidak ditemukan |
| `409` | Conflict | Data duplikat (email, kuota) |
| `422` | Quota Exceeded | Kuota maintenance habis |
| `500` | Internal Server Error | Error tak terduga |

---

## 📂 Struktur Project

```
ticketing-backend/
├── pom.xml
├── README.md
└── src/main/java/com/itsm/ticketing/
    ├── TicketingBackendApplication.java
    ├── config/
    │   ├── SecurityConfig.java            # Spring Security + RBAC config
    │   └── WebSocketConfig.java           # WebSocket STOMP configuration
    ├── controller/
    │   ├── AuthController.java            # Login & Register
    │   ├── ChatController.java            # WebSocket chat + REST history
    │   ├── ClientController.java          # CRUD client (ADMIN)
    │   ├── ClientQuotaController.java     # CRUD kuota (ADMIN)
    │   ├── TicketController.java          # CRUD ticket (ADMIN+USER)
    │   └── UserController.java            # CRUD user (ADMIN)
    ├── dto/
    │   ├── ApiErrorResponse.java
    │   ├── AuthResponse.java              # JWT token response
    │   ├── ChatMessageRequest.java        # Chat send request
    │   ├── ChatMessageResponse.java       # Chat message response
    │   ├── LoginRequest.java              # Login request
    │   ├── RegisterRequest.java           # Registration request
    │   └── ... (other DTOs)
    ├── entity/
    │   ├── ChatMessage.java               # Chat message entity
    │   ├── Role.java                      # ADMIN, USER
    │   ├── User.java                      # Implements UserDetails
    │   └── ... (other entities)
    ├── exception/
    │   └── GlobalExceptionHandler.java    # Includes 401, 403 handlers
    ├── security/
    │   ├── JwtAuthenticationEntryPoint.java  # 401 handler
    │   ├── JwtAuthenticationFilter.java      # JWT request filter
    │   ├── JwtUtils.java                     # JWT token utilities
    │   └── WebSocketAuthInterceptor.java     # JWT auth for WebSocket
    ├── repository/
    │   ├── ChatMessageRepository.java     # Chat message queries
    │   └── ... (other repositories)
    └── service/
        ├── AuthService.java               # Register & Login logic
        ├── ChatService.java               # Chat send & history logic
        ├── UserService.java               # + UserDetailsService
        └── ... (other services)
```

---

## 🔄 Alur Penggunaan

```
1. Register Admin  →  POST /api/v1/auth/register  (role: ADMIN)
2. Login           →  POST /api/v1/auth/login      (dapatkan token)
3. Buat Client     →  POST /api/v1/clients         (token ADMIN)
4. Buat Kuota      →  POST /api/v1/client-quotas   (token ADMIN)
5. Register User   →  POST /api/v1/auth/register   (role: USER)
6. Login User      →  POST /api/v1/auth/login      (dapatkan token)
7. Buat Ticket     →  POST /api/v1/tickets          (token USER/ADMIN)
8. Connect Chat    →  WebSocket ws://localhost:8080/ws (STOMP + JWT)
9. Kirim Pesan     →  STOMP /app/chat.send           (real-time)
10. Histori Chat   →  GET /api/v1/chat/{ticketId}    (REST)
```

---

## 📜 License

This project is for internal/educational purposes.
