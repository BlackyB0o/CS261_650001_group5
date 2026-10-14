# Task04: U11 - Define Details, Design UI, API, DB Schema & Sequence Diagram

## 1. User Story Details (U11)
- **Role:** ผู้ใช้งานทั่วไป (Guest / Unauthenticated User)
- **Goal:** ต้องการสมัครสมาชิก (Register) โดยเลือกบทบาทผู้ใช้ และเข้าสู่ระบบ (Login)
- **Benefit:** เพื่อให้สามารถสร้างบัญชีผู้ใช้งาน และเข้าถึงฟีเจอร์สำหรับผู้รับหิ้วหรือผู้ฝากหิ้วในระบบได้
- **Acceptance Criteria:**
  1. ระบบต้องตรวจสอบไม่ให้ใช้ Email หรือ เบอร์โทรศัพท์ที่มีอยู่ในระบบแล้วสมัครซ้ำ
  2. รหัสผ่านต้องมีความยาวอย่างน้อย 8 ตัวอักษร
  3. ต้องระบุเบอร์โทรศัพท์ และเลือกประเภทผู้ใช้งาน (`RUNNER` = ผู้รับหิ้ว / `BUYER` = ผู้ฝากหิ้ว)
  4. ระบบทำการเข้ารหัสรหัสผ่าน (Password Hashing) ก่อนบันทึกลง Database
  5. เมื่อเข้าสู่ระบบสำเร็จ ระบบต้องส่งคืน JWT Access Token เพื่อใช้ยืนยันตัวตนใน API ถัดไป

---

## 2. UI Prototype
- **Figma Design Link:** [Task04 - Register & Login UI Prototype](https://www.figma.com/)

---

## 3. API Specification
### Endpoint 1: Register (สมัครสมาชิก)
- **URL:** `/api/v1/auth/register`
- **Method:** `POST`
- **Content-Type:** `application/json`
- **Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "fullName": "Naphat Thongkao",
  "phoneNumber": "0812345678",
  "role": "RUNNER"
}
```
*(หมายเหตุ: `role` สามารถเลือกเป็น `"RUNNER"` สำหรับผู้รับหิ้ว หรือ `"BUYER"` สำหรับผู้ฝากหิ้ว)*

- **Response (201 Created):**
```json
{
  "status": "SUCCESS",
  "message": "User registered successfully",
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "fullName": "Naphat Thongkao",
    "phoneNumber": "0812345678",
    "role": "RUNNER"
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

## 4. API Class Diagram
```mermaid
classDiagram
    class AuthController {
        +register(RegisterRequestDTO request) ResponseEntity~AuthResponseDTO~
        +login(LoginRequestDTO request) ResponseEntity~AuthResponseDTO~
    }
    class RegisterRequestDTO {
        +String email
        +String password
        +String fullName
        +String phoneNumber
        +String role
    }
    class LoginRequestDTO {
        +String email
        +String password
    }
    class AuthResponseDTO {
        +String token
        +String tokenType
    }

    AuthController ..> RegisterRequestDTO : receives
    AuthController ..> LoginRequestDTO : receives
    AuthController ..> AuthResponseDTO : returns
```

---

## 5. DTO Class Specification & Diagram
### DTO Specifications
1. **`RegisterRequestDTO`**
   - `email`: String (Not Null, Valid Email)
   - `password`: String (Not Null, Min 8 characters)
   - `fullName`: String (Not Null)
   - `phoneNumber`: String (Not Null, Valid Phone Number)
   - `role`: String (Not Null, "RUNNER" หรือ "BUYER")
2. **`LoginRequestDTO`**
   - `email`: String (Not Null)
   - `password`: String (Not Null)
3. **`AuthResponseDTO`**
   - `token`: String
   - `tokenType`: String ("Bearer")

### DTO Class Diagram
```mermaid
classDiagram
    class RegisterRequestDTO {
        -String email
        -String password
        -String fullName
        -String phoneNumber
        -String role
        +getEmail() String
        +getPassword() String
        +getFullName() String
        +getPhoneNumber() String
        +getRole() String
    }

    class LoginRequestDTO {
        -String email
        -String password
        +getEmail() String
        +getPassword() String
    }

    class AuthResponseDTO {
        -String token
        -String tokenType
        +getToken() String
        +getTokenType() String
    }
```

---

## 6. DAO / Repository Specification & Diagram
- **Interface:** `UserRepository` (extends `JpaRepository<User, Long>`)
- **Methods:**
  - `Optional<User> findByEmail(String email)` - ดึงข้อมูลผู้ใช้จาก Email
  - `boolean existsByEmail(String email)` - ตรวจสอบว่ามี Email นี้ในระบบแล้วหรือยัง
  - `boolean existsByPhoneNumber(String phoneNumber)` - ตรวจสอบว่ามีเบอร์โทรศัพท์นี้ในระบบแล้วหรือยัง
  - `User save(User user)` - บันทึกข้อมูลสมาชิกใหม่ลง Database (Register)

```mermaid
classDiagram
    class UserRepository {
        <<interface>>
        +findByEmail(String email) Optional~User~
        +existsByEmail(String email) boolean
        +existsByPhoneNumber(String phoneNumber) boolean
        +save(User user) User
    }
```

---

## 7. ER Diagram (DB Schema)
```mermaid
erDiagram
    USERS {
        bigint id PK
        varchar email UK "NOT NULL"
        varchar password_hash "NOT NULL"
        varchar full_name "NOT NULL"
        varchar phone_number UK "NOT NULL"
        varchar role "NOT NULL DEFAULT 'BUYER'"
        timestamp created_at "DEFAULT CURRENT_TIMESTAMP"
    }
```

---

## 8. Extended API Class Diagram (Full Layered Architecture)
```mermaid
classDiagram
    class AuthController {
        -AuthService authService
        +register(RegisterRequestDTO request) ResponseEntity~AuthResponseDTO~
        +login(LoginRequestDTO request) ResponseEntity~AuthResponseDTO~
    }

    class AuthService {
        <<interface>>
        +register(RegisterRequestDTO request) AuthResponseDTO
        +login(LoginRequestDTO request) AuthResponseDTO
    }

    class AuthServiceImpl {
        -UserRepository userRepository
        -PasswordEncoder passwordEncoder
        -JwtTokenProvider jwtTokenProvider
        +register(RegisterRequestDTO request) AuthResponseDTO
        +login(LoginRequestDTO request) AuthResponseDTO
    }

    class UserRepository {
        <<interface>>
        +findByEmail(String email) Optional~User~
        +existsByEmail(String email) boolean
        +existsByPhoneNumber(String phoneNumber) boolean
        +save(User user) User
    }

    class User {
        -Long id
        -String email
        -String passwordHash
        -String fullName
        -String phoneNumber
        -String role
        -LocalDateTime createdAt
    }

    class RegisterRequestDTO {
        -String email
        -String password
        -String fullName
        -String phoneNumber
        -String role
    }

    class LoginRequestDTO {
        -String email
        -String password
    }

    class AuthResponseDTO {
        -String token
        -String tokenType
    }

    AuthController --> AuthService : uses
    AuthServiceImpl ..|> AuthService : implements
    AuthServiceImpl --> UserRepository : uses
    UserRepository ..> User : manages
    AuthServiceImpl ..> RegisterRequestDTO : accepts
    AuthServiceImpl ..> LoginRequestDTO : accepts
    AuthServiceImpl ..> AuthResponseDTO : creates
```

---

## 9. Sequence Diagrams
### Sequence Diagram 1: Register (สมัครสมาชิก)
```mermaid
sequenceDiagram
    autonumber
    actor Client as Frontend
    participant Controller as AuthController
    participant Service as AuthService
    participant DAO as UserRepository
    participant DB as PostgreSQL

    Client->>Controller: POST /api/v1/auth/register (RegisterRequestDTO)
    Controller->>Service: register(RegisterRequestDTO)
    Service->>DAO: existsByEmail(email)
    DAO->>DB: SELECT COUNT(*) FROM users WHERE email = ?
    DB-->>DAO: 0 (Not Found)
    DAO-->>Service: false
    Service->>DAO: existsByPhoneNumber(phoneNumber)
    DAO->>DB: SELECT COUNT(*) FROM users WHERE phone_number = ?
    DB-->>DAO: 0 (Not Found)
    DAO-->>Service: false
    Service->>Service: Hash Password (BCrypt)
    Service->>DAO: save(User entity)
    DAO->>DB: INSERT INTO users (email, password_hash, full_name, phone_number, role) ...
    DB-->>DAO: Saved User
    DAO-->>Service: User
    Service-->>Controller: AuthResponseDTO
    Controller-->>Client: 201 Created (AuthResponseDTO)
```

### Sequence Diagram 2: Login (เข้าสู่ระบบ)
```mermaid
sequenceDiagram
    autonumber
    actor Client as Frontend
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