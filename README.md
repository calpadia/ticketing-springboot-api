# 🎫 B2B ITSM Ticketing System — Backend API

Sistem ticketing untuk manajemen IT Service Management (ITSM) berbasis B2B. Backend dibangun dengan Spring Boot dan PostgreSQL, mendukung pembuatan ticket dengan validasi kuota maintenance (Preventive & Corrective Maintenance).

---

## 📋 Daftar Isi

- [Tech Stack](#-tech-stack)
- [Prasyarat](#-prasyarat)
- [Cara Menjalankan](#-cara-menjalankan)
- [Konfigurasi](#-konfigurasi)
- [Database Schema](#-database-schema)
- [API Endpoints](#-api-endpoints)
  - [User API](#-user-api)
  - [Client API](#-client-api)
  - [Client Quota API](#-client-quota-api)
  - [Ticket API](#-ticket-api)
- [Enum Reference](#-enum-reference)
- [Error Handling](#-error-handling)

---

## 🛠 Tech Stack

| Teknologi | Versi |
|-----------|-------|
| Java | 17+ |
| Spring Boot | 3.2.5 |
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
git clone <repository-url>
cd ticketing-backend

# 2. Konfigurasi database di application.properties (lihat bagian Konfigurasi)

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
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.show-sql=true

# Server
server.port=8080
```

---

## 🗄 Database Schema

### Entity Relationship

```
┌─────────────┐       ┌─────────────────┐       ┌─────────────┐
│   clients    │       │     tickets      │       │    users     │
├─────────────┤       ├─────────────────┤       ├─────────────┤
│ id (PK)     │──┐    │ id (PK)         │    ┌──│ id (PK)     │
│ company_name│  │    │ ticket_number   │    │  │ name        │
│ is_active   │  ├───>│ client_id (FK)  │    │  │ email       │
└─────────────┘  │    │ requester_id(FK)│<───┘  │ password    │
                 │    │ title           │       │ role        │
┌─────────────┐  │    │ description     │       └─────────────┘
│client_quotas│  │    │ status          │
├─────────────┤  │    │ priority        │
│ id (PK)     │  │    │ maintenance_type│
│ client_id   │──┘    │ created_at      │
│ year        │       └─────────────────┘
│ pm_quota    │
│ cm_quota    │
│ pm_used     │
│ cm_used     │
└─────────────┘
```

---

## 📡 API Endpoints

**Base URL:** `http://localhost:8080`

### Ringkasan Seluruh API

| Method | Endpoint | Deskripsi |
|--------|----------|-----------|
| **USER** | | |
| `POST` | `/api/v1/users` | Buat user baru |
| `GET` | `/api/v1/users` | Ambil semua user |
| `GET` | `/api/v1/users/{id}` | Ambil user berdasarkan ID |
| `PUT` | `/api/v1/users/{id}` | Update user |
| `DELETE` | `/api/v1/users/{id}` | Hapus user |
| **CLIENT** | | |
| `POST` | `/api/v1/clients` | Buat client baru |
| `GET` | `/api/v1/clients` | Ambil semua client |
| `GET` | `/api/v1/clients/{id}` | Ambil client berdasarkan ID |
| `PUT` | `/api/v1/clients/{id}` | Update client |
| `DELETE` | `/api/v1/clients/{id}` | Hapus client |
| **CLIENT QUOTA** | | |
| `POST` | `/api/v1/client-quotas` | Buat kuota baru |
| `GET` | `/api/v1/client-quotas` | Ambil semua kuota |
| `GET` | `/api/v1/client-quotas/{id}` | Ambil kuota berdasarkan ID |
| `GET` | `/api/v1/client-quotas/client/{clientId}/year/{year}` | Ambil kuota berdasarkan client & tahun |
| `PUT` | `/api/v1/client-quotas/{id}` | Update kuota |
| `DELETE` | `/api/v1/client-quotas/{id}` | Hapus kuota |
| **TICKET** | | |
| `POST` | `/api/v1/tickets` | Buat ticket baru |
| `GET` | `/api/v1/tickets` | Ambil semua ticket |
| `GET` | `/api/v1/tickets/{id}` | Ambil ticket berdasarkan ID |
| `GET` | `/api/v1/tickets/number/{ticketNumber}` | Ambil ticket berdasarkan nomor |

---

### 👤 User API

#### `POST /api/v1/users` — Buat User Baru

**Request Body:**
```json
{
  "name": "John Doe",
  "email": "john.doe@example.com",
  "password": "securePassword123",
  "role": "USER"
}
```

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|------------|
| `name` | `string` | ✅ | Nama lengkap |
| `email` | `string` | ✅ | Email (harus unik & valid) |
| `password` | `string` | ✅ | Password |
| `role` | `string` | ✅ | `USER`, `AGENT`, atau `MANAGER` |

**Response — `201 Created`:**
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john.doe@example.com",
  "role": "USER"
}
```

> ⚠️ Password **tidak** dikembalikan di response untuk keamanan.

**cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john.doe@example.com",
    "password": "securePassword123",
    "role": "USER"
  }'
```

---

#### `GET /api/v1/users` — Ambil Semua User

**Response — `200 OK`:**
```json
[
  {
    "id": 1,
    "name": "John Doe",
    "email": "john.doe@example.com",
    "role": "USER"
  },
  {
    "id": 2,
    "name": "Jane Admin",
    "email": "jane.admin@example.com",
    "role": "MANAGER"
  }
]
```

**cURL:**
```bash
curl http://localhost:8080/api/v1/users
```

---

#### `GET /api/v1/users/{id}` — Ambil User by ID

**Response — `200 OK`:**
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john.doe@example.com",
  "role": "USER"
}
```

**cURL:**
```bash
curl http://localhost:8080/api/v1/users/1
```

---

#### `PUT /api/v1/users/{id}` — Update User

**Request Body:** Sama seperti Create User.

**Response — `200 OK`:** Sama seperti response Get User.

**cURL:**
```bash
curl -X PUT http://localhost:8080/api/v1/users/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Updated",
    "email": "john.updated@example.com",
    "password": "newPassword456",
    "role": "AGENT"
  }'
```

---

#### `DELETE /api/v1/users/{id}` — Hapus User

**Response — `204 No Content`**

**cURL:**
```bash
curl -X DELETE http://localhost:8080/api/v1/users/1
```

---

### 🏢 Client API

#### `POST /api/v1/clients` — Buat Client Baru

**Request Body:**
```json
{
  "companyName": "PT Contoh Perusahaan"
}
```

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|------------|
| `companyName` | `string` | ✅ | Nama perusahaan |

**Response — `201 Created`:**
```json
{
  "id": 1,
  "companyName": "PT Contoh Perusahaan",
  "isActive": true
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/clients \
  -H "Content-Type: application/json" \
  -d '{"companyName": "PT Contoh Perusahaan"}'
```

---

#### `GET /api/v1/clients` — Ambil Semua Client

**Response — `200 OK`:**
```json
[
  {
    "id": 1,
    "companyName": "PT Contoh Perusahaan",
    "isActive": true
  }
]
```

**cURL:**
```bash
curl http://localhost:8080/api/v1/clients
```

---

#### `GET /api/v1/clients/{id}` — Ambil Client by ID

**Response — `200 OK`:**
```json
{
  "id": 1,
  "companyName": "PT Contoh Perusahaan",
  "isActive": true
}
```

**cURL:**
```bash
curl http://localhost:8080/api/v1/clients/1
```

---

#### `PUT /api/v1/clients/{id}` — Update Client

**Request Body:**
```json
{
  "companyName": "PT Contoh Updated"
}
```

**Response — `200 OK`:** Sama seperti response Get Client.

**cURL:**
```bash
curl -X PUT http://localhost:8080/api/v1/clients/1 \
  -H "Content-Type: application/json" \
  -d '{"companyName": "PT Contoh Updated"}'
```

---

#### `DELETE /api/v1/clients/{id}` — Hapus Client

**Response — `204 No Content`**

**cURL:**
```bash
curl -X DELETE http://localhost:8080/api/v1/clients/1
```

---

### 📊 Client Quota API

#### `POST /api/v1/client-quotas` — Buat Kuota Baru

**Request Body:**
```json
{
  "clientId": 1,
  "year": 2026,
  "pmQuota": 12,
  "cmQuota": 24
}
```

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|------------|
| `clientId` | `number` | ✅ | ID client yang terdaftar |
| `year` | `number` | ✅ | Tahun kuota |
| `pmQuota` | `number` | ✅ | Jumlah kuota PM (≥ 0) |
| `cmQuota` | `number` | ✅ | Jumlah kuota CM (≥ 0) |

**Response — `201 Created`:**
```json
{
  "id": 1,
  "clientId": 1,
  "clientCompanyName": "PT Contoh Perusahaan",
  "year": 2026,
  "pmQuota": 12,
  "cmQuota": 24,
  "pmUsed": 0,
  "cmUsed": 0
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/client-quotas \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": 1,
    "year": 2026,
    "pmQuota": 12,
    "cmQuota": 24
  }'
```

---

#### `GET /api/v1/client-quotas` — Ambil Semua Kuota

**Response — `200 OK`:**
```json
[
  {
    "id": 1,
    "clientId": 1,
    "clientCompanyName": "PT Contoh Perusahaan",
    "year": 2026,
    "pmQuota": 12,
    "cmQuota": 24,
    "pmUsed": 2,
    "cmUsed": 5
  }
]
```

**cURL:**
```bash
curl http://localhost:8080/api/v1/client-quotas
```

---

#### `GET /api/v1/client-quotas/{id}` — Ambil Kuota by ID

**cURL:**
```bash
curl http://localhost:8080/api/v1/client-quotas/1
```

---

#### `GET /api/v1/client-quotas/client/{clientId}/year/{year}` — Ambil Kuota by Client & Tahun

**cURL:**
```bash
curl http://localhost:8080/api/v1/client-quotas/client/1/year/2026
```

**Response — `200 OK`:**
```json
{
  "id": 1,
  "clientId": 1,
  "clientCompanyName": "PT Contoh Perusahaan",
  "year": 2026,
  "pmQuota": 12,
  "cmQuota": 24,
  "pmUsed": 2,
  "cmUsed": 5
}
```

---

#### `PUT /api/v1/client-quotas/{id}` — Update Kuota

Hanya mengubah nilai `pmQuota` dan `cmQuota`. Nilai `pmUsed` dan `cmUsed` tidak diubah.

**Request Body:**
```json
{
  "clientId": 1,
  "year": 2026,
  "pmQuota": 18,
  "cmQuota": 30
}
```

**cURL:**
```bash
curl -X PUT http://localhost:8080/api/v1/client-quotas/1 \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": 1,
    "year": 2026,
    "pmQuota": 18,
    "cmQuota": 30
  }'
```

---

#### `DELETE /api/v1/client-quotas/{id}` — Hapus Kuota

**Response — `204 No Content`**

**cURL:**
```bash
curl -X DELETE http://localhost:8080/api/v1/client-quotas/1
```

---

### 🎫 Ticket API

#### `POST /api/v1/tickets` — Buat Ticket Baru

Membuat ticket baru dengan validasi:
- Client dan requester harus terdaftar
- Kuota maintenance (PM/CM) untuk tahun berjalan harus mencukupi
- Nomor ticket di-generate otomatis: `TKT-YYYYMMDD-XXX`

**Request Body:**
```json
{
  "title": "Server tidak bisa diakses",
  "description": "Server produksi down sejak pukul 10:00 WIB",
  "priority": "L1",
  "maintenanceType": "CM",
  "clientId": 1,
  "requesterId": 1
}
```

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|------------|
| `title` | `string` | ✅ | Judul ticket |
| `description` | `string` | ✅ | Deskripsi detail masalah |
| `priority` | `string` | ✅ | `L1`, `L2`, `L3`, atau `L4` |
| `maintenanceType` | `string` | ✅ | `PM` atau `CM` |
| `clientId` | `number` | ✅ | ID client yang terdaftar |
| `requesterId` | `number` | ✅ | ID user pembuat request |

**Response — `201 Created`:**
```json
{
  "id": 1,
  "ticketNumber": "TKT-20260522-001",
  "title": "Server tidak bisa diakses",
  "description": "Server produksi down sejak pukul 10:00 WIB",
  "status": "OPEN",
  "priority": "L1",
  "maintenanceType": "CM",
  "clientId": 1,
  "clientCompanyName": "PT Contoh Perusahaan",
  "requesterId": 1,
  "requesterName": "John Doe",
  "createdAt": "2026-05-22T10:30:00"
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Server tidak bisa diakses",
    "description": "Server produksi down sejak pukul 10:00 WIB",
    "priority": "L1",
    "maintenanceType": "CM",
    "clientId": 1,
    "requesterId": 1
  }'
```

---

#### `GET /api/v1/tickets` — Ambil Semua Ticket

**cURL:**
```bash
curl http://localhost:8080/api/v1/tickets
```

---

#### `GET /api/v1/tickets/{id}` — Ambil Ticket by ID

**cURL:**
```bash
curl http://localhost:8080/api/v1/tickets/1
```

---

#### `GET /api/v1/tickets/number/{ticketNumber}` — Ambil Ticket by Nomor

**cURL:**
```bash
curl http://localhost:8080/api/v1/tickets/number/TKT-20260522-001
```

---

## 📖 Enum Reference

### Priority — Level Prioritas

| Nilai | Keterangan |
|-------|------------|
| `L1` | Level 1 — Urgent / Critical |
| `L2` | Level 2 — High |
| `L3` | Level 3 — Medium |
| `L4` | Level 4 — Low |

### MaintenanceType — Tipe Maintenance

| Nilai | Keterangan |
|-------|------------|
| `PM` | Preventive Maintenance — pemeliharaan berkala |
| `CM` | Corrective Maintenance — perbaikan kerusakan |

### TicketStatus — Status Ticket

| Nilai | Keterangan |
|-------|------------|
| `OPEN` | Ticket baru dibuat |
| `IN_PROGRESS` | Sedang dikerjakan |
| `RESOLVED` | Masalah telah diselesaikan |
| `CLOSED` | Ticket ditutup |

### Role — Role User

| Nilai | Keterangan |
|-------|------------|
| `USER` | Pengguna biasa / requester |
| `AGENT` | Agen IT support |
| `MANAGER` | Manajer / supervisor |

---

## ❌ Error Handling

Semua error menggunakan format response yang konsisten:

### Format Error Response

```json
{
  "status": 400,
  "error": "Error Type",
  "message": "Deskripsi error",
  "timestamp": "2026-05-22T10:30:00",
  "validationErrors": null
}
```

### Daftar Error

| Status | Error | Kondisi |
|--------|-------|---------|
| `400` | Validation Failed | Field required tidak diisi / format tidak valid |
| `404` | Not Found | Resource (user/client/ticket) tidak ditemukan |
| `409` | Conflict | Data duplikat (email sudah terdaftar, kuota sudah ada) |
| `422` | Quota Exceeded | Kuota maintenance client sudah habis |
| `500` | Internal Server Error | Error tak terduga di server |

#### Contoh: `400 Bad Request` — Validation Failed
```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "One or more fields have invalid values",
  "timestamp": "2026-05-22T10:30:00",
  "validationErrors": {
    "title": "Title is required",
    "email": "Email must be valid"
  }
}
```

#### Contoh: `404 Not Found`
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "User not found with ID: 99",
  "timestamp": "2026-05-22T10:30:00"
}
```

#### Contoh: `409 Conflict` — Duplicate Data
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Email already registered: john@example.com",
  "timestamp": "2026-05-22T10:30:00"
}
```

#### Contoh: `422 Unprocessable Entity` — Quota Exceeded
```json
{
  "status": 422,
  "error": "Quota Exceeded",
  "message": "Kuota CM tidak mencukupi untuk client: PT Contoh (Used: 10/10)",
  "timestamp": "2026-05-22T10:30:00"
}
```

---

## 📂 Struktur Project

```
ticketing-backend/
├── pom.xml
├── README.md
└── src/main/java/com/itsm/ticketing/
    ├── TicketingBackendApplication.java
    ├── controller/
    │   ├── ClientController.java          # CRUD client
    │   ├── ClientQuotaController.java     # CRUD kuota client
    │   ├── TicketController.java          # CRUD ticket
    │   └── UserController.java            # CRUD user
    ├── dto/
    │   ├── ApiErrorResponse.java          # Format error response
    │   ├── ClientQuotaResponse.java       # Response kuota
    │   ├── ClientResponse.java            # Response client
    │   ├── CreateClientQuotaRequest.java  # Request buat kuota
    │   ├── CreateClientRequest.java       # Request buat client
    │   ├── CreateTicketRequest.java       # Request buat ticket
    │   ├── CreateUserRequest.java         # Request buat user
    │   ├── TicketResponse.java            # Response ticket
    │   └── UserResponse.java             # Response user
    ├── entity/
    │   ├── Client.java
    │   ├── ClientQuota.java
    │   ├── MaintenanceType.java           # Enum PM/CM
    │   ├── Priority.java                  # Enum L1-L4
    │   ├── Role.java                      # Enum USER/AGENT/MANAGER
    │   ├── Ticket.java
    │   ├── TicketStatus.java              # Enum status ticket
    │   └── User.java
    ├── exception/
    │   ├── GlobalExceptionHandler.java    # Handler error global
    │   ├── QuotaExceededException.java
    │   └── ResourceNotFoundException.java
    ├── repository/
    │   ├── ClientQuotaRepository.java
    │   ├── ClientRepository.java
    │   ├── TicketRepository.java
    │   └── UserRepository.java
    └── service/
        ├── ClientQuotaService.java        # Logic kuota
        ├── ClientService.java             # Logic client
        ├── TicketService.java             # Logic ticket
        └── UserService.java              # Logic user
```

---

## 📝 Business Rules

1. **Ticket Number**: Auto-generated format `TKT-YYYYMMDD-XXX` (contoh: `TKT-20260522-001`)
2. **Quota Validation**: Setiap pembuatan ticket mengecek kuota maintenance client tahun berjalan
3. **Status Default**: Ticket baru selalu `OPEN`
4. **Email Unique**: Setiap user harus memiliki email yang unik
5. **Quota Unique**: Kombinasi `client_id` + `year` harus unik
6. **Password Hidden**: Password tidak dikembalikan di response API

---

## 🔄 Alur Penggunaan

```
1. Buat Client    →  POST /api/v1/clients
2. Buat User      →  POST /api/v1/users
3. Buat Kuota     →  POST /api/v1/client-quotas
4. Buat Ticket    →  POST /api/v1/tickets
```

> **Penting:** Sebelum membuat ticket, pastikan sudah ada data **Client**, **User**, dan **Client Quota** untuk tahun berjalan.

---

## 📜 License

This project is for internal/educational purposes.
