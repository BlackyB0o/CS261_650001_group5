# Task04: U11 - Define Details, Design UI, API, DB Schema & Sequence Diagram

## 1. User Story Details (U11)
- **Role:** ผู้ใช้งานทั่วไป (Guest / Unauthenticated User)
- **Goal:** ต้องการสมัครสมาชิก (Register) และเข้าสู่ระบบ (Login)
- **Benefit:** เพื่อให้สามารถสร้างบัญชีผู้ใช้งาน และเข้าถึงฟีเจอร์สำหรับสมาชิกในระบบได้
- **Acceptance Criteria:**
  1. ระบบต้องตรวจสอบไม่ให้ใช้ Email ที่มีอยู่ในระบบแล้วสมัครซ้ำ
  2. รหัสผ่านต้องมีความยาวอย่างน้อย 8 ตัวอักษร
  3. ระบบทำการเข้ารหัสรหัสผ่าน (Password Hashing) ก่อนบันทึกลง Database
  4. เมื่อเข้าสู่ระบบสำเร็จ ระบบต้องส่งคืน JWT Access Token เพื่อใช้ยืนยันตัวตนใน API ถัดไป

---

## 2. API Specification
### Endpoint 1: Register (สมัครสมาชิก)
- **URL:** `/api/v1/auth/register`
- **Method:** `POST`
- **Content-Type:** `application/json`
- **Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "fullName": "Naphat Thongkao"
}
```
- **Response (201 Created):**
```json
{
  "status": "SUCCESS",
  "message": "User registered successfully",
  "data": {
    "userId": 1,
    "email": "user@example.com"
  }
}
```

### Endpoint 2: Login (เข้าสู่ระบบ)
- **URL:** `/api/v1/auth/login`
- **Method:** `POST`
- **Content-Type:** `application/json`
- **Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```
- **Response (200 OK):**
```json
{
  "status": "SUCCESS",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsIn...",
    "tokenType": "Bearer"
  }
}
```

---

## 3. DTO Class Specification
1. **`RegisterRequestDTO`**
   - `email`: String (Not Null, Valid Email)
   - `password`: String (Not Null, Min 8 characters)
   - `fullName`: String (Not Null)
2. **`LoginRequestDTO`**
   - `email`: String (Not Null)
   - `password`: String (Not Null)
3. **`AuthResponseDTO`**
   - `token`: String
   - `tokenType`: String ("Bearer")

---

## 4. DAO / Repository Specification
- **Interface:** `UserRepository` (extends `JpaRepository<User, Long>`)
- **Methods:**
  - `Optional<User> findByEmail(String email)`
  - `boolean existsByEmail(String email)`

---

## 5. ER Diagram (DB Schema)
**Table Name:** `users`
| Column Name | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | รหัสอ้างอิงผู้ใช้ |
| `email` | VARCHAR(255) | UNIQUE, NOT NULL | อีเมลผู้ใช้งาน |
| `password_hash` | VARCHAR(255) | NOT NULL | รหัสผ่านที่ Hash แล้ว |
| `full_name` | VARCHAR(100) | NOT NULL | ชื่อ-นามสกุล |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | วันเวลาที่สมัคร |

---

## 6. Sequence Diagram Code (Mermaid)
```mermaid
sequenceDiagram
    autonumber
    actor Client as Frontend Client
    participant Controller as AuthController
    participant Service as AuthService
    participant DAO as UserRepository
    participant DB as PostgreSQL

    Client->>Controller: POST /api/v1/auth/login (LoginRequestDTO)
    Controller->>Service: login(LoginRequestDTO)
    Service->>DAO: findByEmail(email)
    DAO->>DB: SELECT * FROM users WHERE email = ?
    DB-->>DAO: User Data
    DAO-->>Service: Optional<User>
    Service->>Service: Verify Password Hash (BCrypt)
    Service->>Service: Generate JWT Token
    Service-->>Controller: AuthResponseDTO
    Controller-->>Client: 200 OK (AuthResponseDTO)
```