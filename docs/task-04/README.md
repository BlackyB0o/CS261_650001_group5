# Task04: U11 - Define Details, Design UI, API, DB Schema & Sequence Diagram

> ใช้ร่วมกับ [API Conventions](../common/API-CONVENTIONS.md) (รูปแบบ response, JWT, enum) และ [ERD](../database/ERD.md) (ตาราง `USERS`)

## 1. User Story Details (U11)
- **Role:** ผู้ใช้งานทั่วไป (Guest / Unauthenticated User)
- **Goal:** ต้องการสมัครสมาชิก (Register) โดยบังคับเลือกบทบาทเป็นผู้รับหิ้วหรือผู้ฝากหิ้ว และเข้าสู่ระบบ (Login) ด้วย Email หรือ เบอร์โทรศัพท์
- **Benefit:** เพื่อให้สามารถสร้างบัญชีผู้ใช้งาน และเข้าถึงฟีเจอร์ตามบทบาทในระบบได้อย่างถูกต้อง
- **Acceptance Criteria:**
  1. การสมัครสมาชิก (Register): บังคับระบุทั้ง Email และ เบอร์โทรศัพท์ โดยระบบต้องตรวจสอบไม่ให้ซ้ำกับที่มีในระบบ
  2. ต้องระบุชื่อ-นามสกุล และ รหัสผ่าน (ความยาวอย่างน้อย 8 ตัวอักษร)
  3. **บังคับเลือกบทบาทผู้ใช้งาน (Mandatory Role Selection):** ผู้ใช้ **ต้องเลือก** บทบาทอย่างใดอย่างหนึ่งระหว่าง `RUNNER` (ผู้รับหิ้ว) หรือ `BUYER` (ผู้ฝากหิ้ว) เสมอ (ห้ามปล่อยว่าง)
  4. ระบบทำการเข้ารหัสรหัสผ่าน (Password Hashing) ก่อนบันทึกลง Database
  5. การเข้าสู่ระบบ (Login): สามารถระบุ **Email หรือ เบอร์โทรศัพท์** อย่างใดอย่างหนึ่ง คู่กับ รหัสผ่านได้
  6. เมื่อเข้าสู่ระบบสำเร็จ ระบบต้องส่งคืน JWT Access Token เพื่อใช้ยืนยันตัวตนใน API ถัดไป
  7. หากกรอกข้อมูลไม่ครบหรือไม่ถูกต้อง ระบบต้องแสดงข้อความแจ้งเตือนชัดเจนที่ช่องนั้น ๆ และไม่บันทึกข้อมูลลงระบบ
- **หมายเหตุ:** ผู้ดูแลระบบ (Admin) ไม่ได้สมัครผ่านหน้านี้ รับสมัครเฉพาะ `RUNNER` และ `BUYER`

---

## 2. UI Prototype
- **Figma Design Link:** [Task04 - Register & Login UI Prototype](https://www.figma.com/)

---

## 3. API Specification

> **รูปแบบ Response ที่ใช้ร่วมกันทุก endpoint (`ApiResponseDTO<T>`)** ดูรายละเอียดที่ [API Conventions](../common/API-CONVENTIONS.md)
> - สำเร็จ: `{ "status": "SUCCESS", "message": "...", "data": ... }`
> - ผิดพลาด: `{ "status": "ERROR", "message": "...", "errors": [ { "field": "...", "message": "..." } ] }`
>   (`errors` ใช้เมื่อต้องบอกว่าช่องไหนผิด ตามเกณฑ์ข้อ 7)

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
*(หมายเหตุ: ฟิลด์ `role` **บังคับส่ง** โดยเลือกได้เพียง 2 ค่าเท่านั้น คือ `"RUNNER"` สำหรับผู้รับหิ้ว หรือ `"BUYER"` สำหรับผู้ฝากหิ้ว)*

- **Response (201 Created):** ข้อมูลผู้ใช้ที่สมัครสำเร็จ (`RegisterResponseDTO`) ไม่ส่งรหัสผ่านกลับ
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

- **Response (400 Bad Request):** ข้อมูลไม่ครบหรือผิด format (ไม่บันทึกข้อมูล)
```json
{
  "status": "ERROR",
  "message": "Validation failed",
  "errors": [
    { "field": "phoneNumber", "message": "must be a valid phone number" },
    { "field": "role", "message": "must not be blank" }
  ]
}
```

- **Response (409 Conflict):** Email หรือ เบอร์โทรศัพท์ซ้ำกับที่มีในระบบ
```json
{
  "status": "ERROR",
  "message": "Duplicate data",
  "errors": [
    { "field": "email", "message": "Email is already in use" }
  ]
}
```

### Endpoint 2: Login (เข้าสู่ระบบ)
- **URL:** `/api/v1/auth/login`
- **Method:** `POST`
- **Content-Type:** `application/json`
- **Request Body:**
```json
{
  "identifier": "user@example.com",
  "password": "password123"
}
```
*(หมายเหตุ: `identifier` ระบุได้ทั้ง Email เช่น `"user@example.com"` หรือ เบอร์โทรศัพท์ เช่น `"0812345678"`)*

- **Response (200 OK):**
```json
{
  "status": "SUCCESS",
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsIn...",
    "tokenType": "Bearer"
  }
}
```

- **Response (400 Bad Request):** ไม่กรอก `identifier` หรือ `password`
```json
{
  "status": "ERROR",
  "message": "Validation failed",
  "errors": [
    { "field": "password", "message": "must not be blank" }
  ]
}
```

- **Response (401 Unauthorized):** ไม่พบผู้ใช้หรือรหัสผ่านไม่ถูกต้อง (ใช้ข้อความเดียวกัน ไม่บอกว่าผิดที่ตัวไหน)
```json
{
  "status": "ERROR",
  "message": "Invalid email/phone number or password"
}
```

- **JWT ที่ออกให้:** ต้องมีข้อมูล `userId` และ `role` (`RUNNER` / `BUYER`) เพราะ endpoint ของ task อื่นใช้ค่านี้ระบุตัวผู้ใช้และตรวจสิทธิ์ (ดู [API Conventions](../common/API-CONVENTIONS.md))

---

## 4. API Class Diagram
```mermaid
classDiagram
    class AuthController {
        +register(RegisterRequestDTO request) ResponseEntity~ApiResponseDTO~
        +login(LoginRequestDTO request) ResponseEntity~ApiResponseDTO~
    }
    class RegisterRequestDTO {
        +String email
        +String password
        +String fullName
        +String phoneNumber
        +String role
    }
    class LoginRequestDTO {
        +String identifier
        +String password
    }
    class RegisterResponseDTO {
        +Long userId
        +String email
        +String fullName
        +String phoneNumber
        +String role
    }
    class AuthResponseDTO {
        +String token
        +String tokenType
    }
    class ApiResponseDTO~T~ {
        +String status
        +String message
        +T data
        +List~FieldErrorDTO~ errors
    }
    class FieldErrorDTO {
        +String field
        +String message
    }

    AuthController ..> RegisterRequestDTO : receives
    AuthController ..> LoginRequestDTO : receives
    AuthController ..> ApiResponseDTO : returns
    ApiResponseDTO ..> RegisterResponseDTO : wraps (register)
    ApiResponseDTO ..> AuthResponseDTO : wraps (login)
    ApiResponseDTO ..> FieldErrorDTO : errors
```

---

## 5. DTO Class Specification & Diagram
### DTO Specifications
1. **`RegisterRequestDTO`**
   - `email`: String (Not Blank, Valid Email)
   - `password`: String (Not Blank, Min 8 characters)
   - `fullName`: String (Not Blank)
   - `phoneNumber`: String (Not Blank, Valid Phone Number เช่น 10 หลัก ขึ้นต้นด้วย 0)
   - `role`: String (Not Blank, Required Choice: `"RUNNER"` หรือ `"BUYER"`)
2. **`LoginRequestDTO`**
   - `identifier`: String (Not Blank - ใส่ Email หรือ เบอร์โทรศัพท์)
   - `password`: String (Not Blank)
3. **`RegisterResponseDTO`** (ตอบกลับหลังสมัครสำเร็จ ไม่มีรหัสผ่าน)
   - `userId`: Long
   - `email`: String
   - `fullName`: String
   - `phoneNumber`: String
   - `role`: String
4. **`AuthResponseDTO`** (ตอบกลับหลัง Login สำเร็จ)
   - `token`: String
   - `tokenType`: String ("Bearer")
5. **`ApiResponseDTO<T>`** (wrapper ของทุก response)
   - `status`: String (`"SUCCESS"` / `"ERROR"`)
   - `message`: String
   - `data`: T (ใช้เมื่อสำเร็จ)
   - `errors`: List&lt;FieldErrorDTO&gt; (ใช้เมื่อผิดพลาดและต้องระบุช่อง ไม่มีค่าเมื่อสำเร็จ)
6. **`FieldErrorDTO`**
   - `field`: String (ชื่อ field ที่ผิด เช่น `"phoneNumber"`)
   - `message`: String (ข้อความแจ้งเตือนที่ช่องนั้น)

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
        -String identifier
        -String password
        +getIdentifier() String
        +getPassword() String
    }

    class RegisterResponseDTO {
        -Long userId
        -String email
        -String fullName
        -String phoneNumber
        -String role
        +getUserId() Long
        +getEmail() String
        +getFullName() String
        +getPhoneNumber() String
        +getRole() String
    }

    class AuthResponseDTO {
        -String token
        -String tokenType
        +getToken() String
        +getTokenType() String
    }

    class ApiResponseDTO~T~ {
        -String status
        -String message
        -T data
        -List~FieldErrorDTO~ errors
        +getStatus() String
        +getMessage() String
        +getData() T
        +getErrors() List~FieldErrorDTO~
    }

    class FieldErrorDTO {
        -String field
        -String message
        +getField() String
        +getMessage() String
    }
```

---

## 6. DAO / Repository Specification & Diagram
- **Interface:** `UserRepository` (extends `JpaRepository<User, Long>`)
- **Methods:**
  - `Optional<User> findByEmailOrPhoneNumber(String email, String phoneNumber)` - ค้นหาผู้ใช้ด้วย Email หรือ เบอร์โทรศัพท์ (ใช้ตอน Login)
  - `boolean existsByEmail(String email)` - เช็ค Email ซ้ำ
  - `boolean existsByPhoneNumber(String phoneNumber)` - เช็คเบอร์โทรศัพท์ซ้ำ
  - `User save(User user)` - บันทึกข้อมูลผู้ใช้ใหม่ (Register)

```mermaid
classDiagram
    class UserRepository {
        <<interface>>
        +findByEmailOrPhoneNumber(String email, String phoneNumber) Optional~User~
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
        varchar role "NOT NULL"
        timestamp created_at "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
    }
```

---

## 8. Extended API Class Diagram (Full Layered Architecture)
```mermaid
classDiagram
    class AuthController {
        -AuthService authService
        +register(RegisterRequestDTO request) ResponseEntity~ApiResponseDTO~
        +login(LoginRequestDTO request) ResponseEntity~ApiResponseDTO~
    }

    class AuthService {
        <<interface>>
        +register(RegisterRequestDTO request) RegisterResponseDTO
        +login(LoginRequestDTO request) AuthResponseDTO
    }

    class AuthServiceImpl {
        -UserRepository userRepository
        -PasswordEncoder passwordEncoder
        -JwtTokenProvider jwtTokenProvider
        +register(RegisterRequestDTO request) RegisterResponseDTO
        +login(LoginRequestDTO request) AuthResponseDTO
    }

    class UserRepository {
        <<interface>>
        +findByEmailOrPhoneNumber(String email, String phoneNumber) Optional~User~
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
        -String identifier
        -String password
    }

    class RegisterResponseDTO {
        -Long userId
        -String email
        -String fullName
        -String phoneNumber
        -String role
    }

    class AuthResponseDTO {
        -String token
        -String tokenType
    }

    class ApiResponseDTO~T~ {
        -String status
        -String message
        -T data
        -List~FieldErrorDTO~ errors
    }

    AuthController --> AuthService : uses
    AuthController ..> ApiResponseDTO : returns
    AuthServiceImpl ..|> AuthService : implements
    AuthServiceImpl --> UserRepository : uses
    UserRepository ..> User : manages
    AuthServiceImpl ..> RegisterRequestDTO : accepts
    AuthServiceImpl ..> LoginRequestDTO : accepts
    AuthServiceImpl ..> RegisterResponseDTO : creates
    AuthServiceImpl ..> AuthResponseDTO : creates
    ApiResponseDTO ..> RegisterResponseDTO : wraps
    ApiResponseDTO ..> AuthResponseDTO : wraps
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
    Controller->>Controller: Validate request (Bean Validation)
    alt ข้อมูลไม่ครบหรือผิด format
        Controller-->>Client: 400 Bad Request (ApiResponseDTO + errors)
    end
    Controller->>Service: register(RegisterRequestDTO)
    Service->>DAO: existsByEmail(email)
    DAO->>DB: SELECT COUNT(*) FROM users WHERE email = ?
    DB-->>DAO: count
    DAO-->>Service: true / false
    alt Email ซ้ำ
        Service-->>Controller: throw DuplicateException (email)
        Controller-->>Client: 409 Conflict (ApiResponseDTO + errors)
    end
    Service->>DAO: existsByPhoneNumber(phoneNumber)
    DAO->>DB: SELECT COUNT(*) FROM users WHERE phone_number = ?
    DB-->>DAO: count
    DAO-->>Service: true / false
    alt เบอร์โทรศัพท์ซ้ำ
        Service-->>Controller: throw DuplicateException (phoneNumber)
        Controller-->>Client: 409 Conflict (ApiResponseDTO + errors)
    end
    Service->>Service: Validate Role (RUNNER / BUYER)
    Service->>Service: Hash Password (BCrypt)
    Service->>DAO: save(User entity)
    DAO->>DB: INSERT INTO users (email, password_hash, full_name, phone_number, role) ...
    DB-->>DAO: Saved User
    DAO-->>Service: User
    Service-->>Controller: RegisterResponseDTO
    Controller-->>Client: 201 Created (ApiResponseDTO wraps RegisterResponseDTO)
```

### Sequence Diagram 2: Login (เข้าสู่ระบบด้วย Email หรือ เบอร์โทรศัพท์)
```mermaid
sequenceDiagram
    autonumber
    actor Client as Frontend
    participant Controller as AuthController
    participant Service as AuthService
    participant DAO as UserRepository
    participant DB as PostgreSQL

    Client->>Controller: POST /api/v1/auth/login (LoginRequestDTO)
    Controller->>Controller: Validate request (Bean Validation)
    alt ไม่กรอก identifier หรือ password
        Controller-->>Client: 400 Bad Request (ApiResponseDTO + errors)
    end
    Controller->>Service: login(LoginRequestDTO)
    Service->>DAO: findByEmailOrPhoneNumber(identifier, identifier)
    DAO->>DB: SELECT * FROM users WHERE email = ? OR phone_number = ?
    DB-->>DAO: User Data
    DAO-->>Service: Optional of User
    Service->>Service: Verify Password Hash (BCrypt)
    alt ไม่พบผู้ใช้ หรือรหัสผ่านไม่ถูกต้อง
        Service-->>Controller: throw InvalidCredentialsException
        Controller-->>Client: 401 Unauthorized (ApiResponseDTO)
    end
    Service->>Service: Generate JWT Token
    Service-->>Controller: AuthResponseDTO
    Controller-->>Client: 200 OK (ApiResponseDTO wraps AuthResponseDTO)
```
