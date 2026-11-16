# Task 53: US10 - Sequence Diagram และ Admin UI สำหรับการระงับบัญชี

## 1. Sequence Diagram

### 1.1 Admin Login (POST /api/v1/auth/admin/login)

```mermaid
sequenceDiagram
    autonumber
    actor Admin as ผู้ดูแลระบบ (Admin)
    participant FE as Admin Login UI
    participant Controller as AuthController
    participant SVC as AuthServiceImpl
    participant UserRepo as UserRepository
    participant DB as Database

    Admin->>FE: 1. กรอกอีเมลและรหัสผ่านแอดมิน (identifier, password)
    FE->>Controller: 2. POST /api/v1/auth/admin/login<br/>Body: { "identifier": "admin@carryhub.com", "password": "..." }
    activate Controller

    Controller->>Controller: 3. ตรวจสอบ @Valid LoginRequestDTO

    alt กรณีไม่ส่ง identifier หรือ password หรือค่าว่าง
        Controller-->>FE: HTTP 400 Bad Request ("Validation failed")
        FE-->>Admin: แสดงแจ้งเตือนกรุณากรอกข้อมูลให้ครบถ้วน
    else กรณีรูปแบบข้อมูลถูกต้อง
        Controller->>SVC: 4. adminLogin(LoginRequestDTO request)
        activate SVC

        SVC->>UserRepo: 5. findByEmail(request.identifier)
        activate UserRepo
        UserRepo->>DB: 6. SELECT * FROM users WHERE email = :identifier
        activate DB
        DB-->>UserRepo: 7. Return Optional<User>
        deactivate DB
        UserRepo-->>SVC: 8. Return Optional<User>
        deactivate UserRepo

        SVC->>SVC: 9. ตรวจสอบว่าพบผู้ใช้, รหัสผ่านถูกต้อง และ user.role == "ADMIN"

        alt กรณีไม่พบอีเมล, รหัสผ่านผิด หรือบัญชีไม่ใช่ ADMIN
            SVC-->>Controller: Throw ApiException.unauthorized("Invalid credentials")
            Controller-->>FE: HTTP 401 Unauthorized ("Invalid credentials")
            FE-->>Admin: แสดงแจ้งเตือน "อีเมลหรือรหัสผ่านไม่ถูกต้อง"
        else กรณีเป็นบัญชี ADMIN และรหัสผ่านถูกต้อง
            SVC->>SVC: 10. สร้าง JWT Token (ไม่ผ่านขั้นตอน OTP)
            SVC-->>Controller: 11. Return AuthResponseDTO (token, userId, fullName)
            deactivate SVC

            Controller-->>FE: 12. HTTP 200 OK (ApiResponseDTO.success("Admin login successful", data))
            deactivate Controller
            FE-->>Admin: 13. บันทึก Token และเข้าสู่หน้า Admin Dashboard
        end
    end
```

### 1.2 Get All Reports & Report Detail (GET /api/v1/admin/reports และ GET /api/v1/admin/reports/{reportId})

```mermaid
sequenceDiagram
    autonumber
    actor Admin as ผู้ดูแลระบบ (Admin)
    participant FE as Admin Dashboard UI
    participant Auth as JWT Auth Filter
    participant Controller as AdminController
    participant SVC as AdminServiceImpl
    participant UserRepo as UserRepository
    participant ReportRepo as ReportRepository
    participant DB as Database

    Admin->>FE: 1. เปิดหน้าดูรายการรายงานทั้งหมด หรือคลิกดูรายละเอียดรายงาน (reportId)
    FE->>Auth: 2. GET /api/v1/admin/reports (หรือ /api/v1/admin/reports/{reportId})<br/>Header: Authorization: Bearer JWT_TOKEN
    activate Auth

    Auth->>Auth: 3. ตรวจสอบ JWT Token และดึง userId

    alt กรณีไม่มี Token หรือ Token ไม่ถูกต้อง/หมดอายุ
        Auth-->>FE: HTTP 401 Unauthorized ("Invalid or missing token")
        FE-->>Admin: Redirect ไปหน้า Admin Login
    else กรณี Token ถูกต้อง
        Auth->>Controller: 4. getReports(userId) / getReport(userId, reportId)
        deactivate Auth
        activate Controller

        Controller->>SVC: 5. ตรวจสอบสิทธิ์แอดมินและดึงข้อมูลรายงาน
        activate SVC

        SVC->>UserRepo: 6. findById(userId)
        activate UserRepo
        UserRepo->>DB: 7. SELECT * FROM users WHERE id = :userId
        activate DB
        DB-->>UserRepo: 8. Return User
        deactivate DB
        UserRepo-->>SVC: 9. Return User
        deactivate UserRepo

        alt กรณี user.suspended == true
            SVC-->>Controller: Throw ApiException.unauthorized("Account suspended")
            Controller-->>FE: HTTP 401 Unauthorized ("Account suspended")
        else กรณี user.role != "ADMIN" (ผู้ใช้ทั่วไปเรียก /admin/*)
            SVC-->>Controller: Throw ApiException.forbidden("Admin access required")
            Controller-->>FE: HTTP 403 Forbidden ("Admin access required")
            FE-->>Admin: แสดงแจ้งเตือน "ไม่มีสิทธิ์เข้าถึงส่วนของผู้ดูแลระบบ"
        else กรณีเป็น ADMIN ถูกต้อง
            alt กรณีดูรายงานทั้งหมด
                SVC->>ReportRepo: 10a. findAllByOrderByCreatedAtDescIdDesc()
                activate ReportRepo
                ReportRepo->>DB: 11a. SELECT r.*, u1, u2, o FROM reports r ... ORDER BY created_at DESC, id DESC
                activate DB
                DB-->>ReportRepo: 12a. Return List<Report>
                deactivate DB
                ReportRepo-->>SVC: 13a. Return List<Report>
                deactivate ReportRepo
                SVC-->>Controller: 14a. Return List<AdminReportResponseDTO>
                Controller-->>FE: 15a. HTTP 200 OK (รายการรายงานเรียงจากใหม่ไปเก่า)
            else กรณีดูรายละเอียดทีละรายงาน (reportId)
                SVC->>ReportRepo: 10b. findWithDetailsById(reportId)
                activate ReportRepo
                ReportRepo->>DB: 11b. SELECT * FROM reports WHERE id = :reportId
                activate DB
                DB-->>ReportRepo: 12b. Return Optional<Report>
                deactivate DB
                ReportRepo-->>SVC: 13b. Return Optional<Report>
                deactivate ReportRepo

                alt กรณีไม่พบรายงาน
                    SVC-->>Controller: Throw ApiException.notFound("Report not found")
                    Controller-->>FE: HTTP 404 Not Found ("Report not found")
                else กรณีพบรายงาน
                    SVC-->>Controller: 14b. Return AdminReportResponseDTO
                    deactivate SVC
                    Controller-->>FE: 15b. HTTP 200 OK (รายละเอียดรายงานพร้อมสถานะ reportedUserSuspended)
                    deactivate Controller
                end
            end
            FE-->>Admin: 16. แสดงข้อมูลรายงานและปุ่มพิจารณา / ระงับบัญชี
        end
    end
```

### 1.3 Review Report Decision (PATCH /api/v1/admin/reports/{reportId}/decision)

```mermaid
sequenceDiagram
    autonumber
    actor Admin as ผู้ดูแลระบบ (Admin)
    participant FE as Admin Dashboard UI
    participant Auth as JWT Auth Filter
    participant Controller as AdminController
    participant SVC as AdminServiceImpl
    participant UserRepo as UserRepository
    participant ReportRepo as ReportRepository
    participant DB as Database

    Admin->>FE: 1. กดอนุมัติ (APPROVED) หรือปฏิเสธ (DECLINED) รายงาน พร้อมกรอก adminNote
    FE->>Auth: 2. PATCH /api/v1/admin/reports/{reportId}/decision<br/>Header: Authorization: Bearer JWT_TOKEN<br/>Body: { "decision": "APPROVED", "adminNote": "..." }
    activate Auth

    Auth->>Auth: 3. ตรวจสอบ JWT Token และ @Valid ReportDecisionRequestDTO

    alt กรณี Token ผิด / ไม่มี Token
        Auth-->>FE: HTTP 401 Unauthorized ("Invalid or missing token")
    else กรณี decision ไม่ใช่ APPROVED/DECLINED หรือ adminNote เกิน 1000 ตัวอักษร
        Auth-->>FE: HTTP 400 Bad Request ("Validation failed")
    else กรณีผ่านการตรวจสอบเบื้องต้น
        Auth->>Controller: 4. reviewReport(userId, reportId, ReportDecisionRequestDTO)
        deactivate Auth
        activate Controller

        Controller->>SVC: 5. reviewReport(userId, reportId, request)
        activate SVC

        SVC->>UserRepo: 6. findById(userId) (ตรวจ role == "ADMIN" และ !suspended)
        activate UserRepo
        UserRepo->>DB: 7. SELECT * FROM users WHERE id = :userId
        activate DB
        DB-->>UserRepo: 8. Return User
        deactivate DB
        UserRepo-->>SVC: 9. Return User
        deactivate UserRepo

        alt กรณีไม่ใช่ ADMIN
            SVC-->>Controller: Throw ApiException.forbidden("Admin access required")
            Controller-->>FE: HTTP 403 Forbidden ("Admin access required")
        else กรณีเป็น ADMIN
            SVC->>ReportRepo: 10. findWithDetailsById(reportId)
            activate ReportRepo
            ReportRepo->>DB: 11. SELECT * FROM reports WHERE id = :reportId
            activate DB
            DB-->>ReportRepo: 12. Return Optional<Report>
            deactivate DB
            ReportRepo-->>SVC: 13. Return Optional<Report>
            deactivate ReportRepo

            alt กรณีไม่พบรายงาน
                SVC-->>Controller: Throw ApiException.notFound("Report not found")
                Controller-->>FE: HTTP 404 Not Found ("Report not found")
            else กรณีรายงานถูกพิจารณาไปแล้ว (status != "PENDING")
                SVC-->>Controller: Throw ApiException.conflict("Report has already been reviewed")
                Controller-->>FE: HTTP 409 Conflict ("Report has already been reviewed")
                FE-->>Admin: แสดงแจ้งเตือน "รายงานนี้ถูกพิจารณาไปแล้ว"
            else กรณีสถานะเป็น PENDING
                SVC->>ReportRepo: 14. อัปเดต status, adminNote, reviewedAt = NOW() & save(report)
                activate ReportRepo
                ReportRepo->>DB: 15. UPDATE reports SET status = :decision, admin_note = :adminNote, reviewed_at = NOW() WHERE id = :reportId
                activate DB
                DB-->>ReportRepo: 16. Return Updated Report
                deactivate DB
                ReportRepo-->>SVC: 17. Return Report
                deactivate ReportRepo

                SVC-->>Controller: 18. Return AdminReportResponseDTO
                deactivate SVC

                Controller-->>FE: 19. HTTP 200 OK (ApiResponseDTO.success("Report reviewed successfully", data))
                deactivate Controller
                FE-->>Admin: 20. อัปเดตสถานะรายงานบนหน้า Dashboard
            end
        end
    end
```

### 1.4 Suspend / Unsuspend User Account (PATCH /api/v1/admin/users/{userId}/suspend)

```mermaid
sequenceDiagram
    autonumber
    actor Admin as ผู้ดูแลระบบ (Admin)
    participant FE as Admin Dashboard UI
    participant Auth as JWT Auth Filter
    participant Controller as AdminController
    participant SVC as AdminServiceImpl
    participant UserRepo as UserRepository
    participant DB as Database

    Admin->>FE: 1. กดปุ่ม "ระงับบัญชี" (suspended: true) หรือ "คืนสิทธิ์บัญชี" (suspended: false)
    FE->>Auth: 2. PATCH /api/v1/admin/users/{userId}/suspend<br/>Header: Authorization: Bearer JWT_TOKEN<br/>Body: { "suspended": true }
    activate Auth

    Auth->>Auth: 3. ตรวจสอบ JWT Token และ @Valid SuspendUserRequestDTO

    alt กรณี Token ผิด / ไม่มี Token
        Auth-->>FE: HTTP 401 Unauthorized ("Invalid or missing token")
    else กรณีไม่ส่งค่า suspended หรือ userId ไม่ใช่ตัวเลข
        Auth-->>FE: HTTP 400 Bad Request ("Validation failed")
    else กรณีผ่านการตรวจสอบเบื้องต้น
        Auth->>Controller: 4. suspendUser(userId, targetUserId, SuspendUserRequestDTO)
        deactivate Auth
        activate Controller

        Controller->>SVC: 5. suspendUser(adminId, targetUserId, request)
        activate SVC

        SVC->>UserRepo: 6. findById(adminId) & findById(targetUserId)
        activate UserRepo
        UserRepo->>DB: 7. SELECT * FROM users WHERE id IN (:adminId, :targetUserId)
        activate DB
        DB-->>UserRepo: 8. Return Admin User & Target User
        deactivate DB
        UserRepo-->>SVC: 9. Return Users
        deactivate UserRepo

        alt กรณีผู้เรียกไม่ใช่ ADMIN
            SVC-->>Controller: Throw ApiException.forbidden("Admin access required")
            Controller-->>FE: HTTP 403 Forbidden ("Admin access required")
        else กรณีไม่พบบัญชีผู้ใช้เป้าหมาย (targetUserId)
            SVC-->>Controller: Throw ApiException.notFound("User not found")
            Controller-->>FE: HTTP 404 Not Found ("User not found")
        else กรณีพยายามระงับบัญชี ADMIN (targetUser.role == "ADMIN")
            SVC-->>Controller: Throw ApiException.forbidden("Admin accounts cannot be suspended")
            Controller-->>FE: HTTP 403 Forbidden ("Admin accounts cannot be suspended")
            FE-->>Admin: แสดงแจ้งเตือน "ไม่สามารถระงับบัญชีแอดมินได้"
        else กรณีดำเนินการได้ถูกต้อง
            SVC->>UserRepo: 10. ตั้งค่า targetUser.setSuspended(request.suspended) & save(targetUser)
            activate UserRepo
            UserRepo->>DB: 11. UPDATE users SET suspended = :suspended WHERE id = :targetUserId
            activate DB
            DB-->>UserRepo: 12. Return Updated User
            deactivate DB
            UserRepo-->>SVC: 13. Return User
            deactivate UserRepo

            SVC-->>Controller: 14. Return UserSuspensionResponseDTO (userId, fullName, suspended)
            deactivate SVC

            Controller-->>FE: 15. HTTP 200 OK ("User suspended successfully" / "User unsuspended successfully")
            deactivate Controller
            FE-->>Admin: 16. อัปเดตสถานะบัญชีบนหน้าจอ (บัญชีที่ถูกระงับจะถูกตัดสิทธิ์ Token ทันที)
        end
    end
```

---

## 2. Test Cases (US10)

| Test Case ID | หัวข้อการทดสอบ (Test Scenario) | เงื่อนไข / Input Data | ผลลัพธ์ที่คาดหวัง (Expected Output) | HTTP Status |
| :--- | :--- | :--- | :--- | :---: |
| **TC-US10-01** | แอดมินเข้าสู่ระบบด้วยอีเมลและรหัสผ่านสำเร็จ | `POST /api/v1/auth/admin/login` + `{ "identifier": "admin@carryhub.com", "password": "AdminPassword123" }` | เข้าสู่ระบบได้ทันทีโดยไม่ผ่าน OTP คืน `"Admin login successful"` พร้อม JWT `token` | `200 OK` |
| **TC-US10-02** | ป้องกันการเข้าสู่ระบบแอดมินเมื่อรหัสผ่านผิด หรือไม่ใช่บัญชีแอดมิน | `POST /api/v1/auth/admin/login` + รหัสผ่านผิด หรืออีเมลของผู้ใช้ทั่วไป (`role = USER`) | ปฏิเสธการเข้าสู่ระบบ คืนค่า Error `"Invalid credentials"` | `401 Unauthorized` |
| **TC-US10-03** | แอดมินดึงข้อมูลรายงานทั้งหมดและดูรายละเอียดแต่ละรายงานสำเร็จ | `GET /api/v1/admin/reports` และ `GET /api/v1/admin/reports/12` + Token แอดมิน | คืนค่ารายการ `AdminReportResponseDTO` เรียงจากใหม่ไปเก่า พร้อมข้อมูลผู้ถูกรายงานและสถานะ `reportedUserSuspended` | `200 OK` |
| **TC-US10-04** | แอดมินพิจารณารายงาน (`APPROVED` / `DECLINED`) พร้อมหมายเหตุสำเร็จ | `PATCH /api/v1/admin/reports/12/decision` + Token แอดมิน + `{ "decision": "APPROVED", "adminNote": "ตรวจสอบหลักฐานแล้ว ผู้รับหิ้วไม่มาส่งจริง" }` | อัปเดตสถานะรายงานเป็น `APPROVED`, บันทึก `adminNote` และ `reviewedAt` คืน `"Report reviewed successfully"` | `200 OK` |
| **TC-US10-05** | ป้องกันการพิจารณารายงานซ้ำที่ถูกตัดสินไปแล้ว | `PATCH /api/v1/admin/reports/12/decision` (รายงานที่ `status` เป็น `APPROVED` หรือ `DECLINED` แล้ว) + Token แอดมิน | ปฏิเสธการแก้ไขซ้ำ คืนค่า Error `"Report has already been reviewed"` | `409 Conflict` |
| **TC-US10-06** | แอดมินสั่งระงับบัญชีผู้ใช้ที่ทำผิดกฎ (`suspended: true`) สำเร็จ | `PATCH /api/v1/admin/users/3/suspend` + Token แอดมิน + `{ "suspended": true }` | อัปเดต `suspended = true`, คืน `"User suspended successfully"`, บัญชี 3 ล็อกอินไม่ได้ (`403 Account suspended`) และ Token เดิมถูกตัดทันที (`401 Account suspended`) | `200 OK` |
| **TC-US10-07** | แอดมินคืนสิทธิ์การใช้งานบัญชี (`suspended: false`) สำเร็จ | `PATCH /api/v1/admin/users/3/suspend` + Token แอดมิน + `{ "suspended": false }` | อัปเดต `suspended = false` คืน `"User unsuspended successfully"` และบัญชี 3 กลับมาใช้งานได้ปกติ | `200 OK` |
| **TC-US10-08** | ป้องกันการระงับบัญชีแอดมิน | `PATCH /api/v1/admin/users/1/suspend` (ID ของแอดมิน) + Token แอดมิน + `{ "suspended": true }` | ปฏิเสธการระงับบัญชี คืนค่า Error `"Admin accounts cannot be suspended"` | `403 Forbidden` |
| **TC-US10-09** | ป้องกันผู้ใช้ทั่วไป (`role = USER`) เข้าถึง Endpoint กลุ่ม `/admin/*` | `GET /api/v1/admin/reports` หรือ `PATCH /api/v1/admin/users/3/suspend` + Token ของผู้ใช้ทั่วไป | ปฏิเสธการเข้าถึง คืนค่า Error `"Admin access required"` | `403 Forbidden` |
| **TC-US10-10** | ผู้ใช้งานทั่วไปตรวจสอบประวัติรายงานของตนเองสำเร็จ | `GET /api/v1/reports/my` + Token ของผู้ใช้งาน | คืนค่ารายการ `ReportResponseDTO` ที่ผู้ใช้คนนี้ส่งเรียงจากใหม่ไปเก่า พร้อมสถานะการพิจารณาของแอดมิน | `200 OK` |
| **TC-US10-11** | ไม่พบข้อมูลรายงานหรือบัญชีผู้ใช้ที่ระบุในระบบ | `GET /api/v1/admin/reports/9999` หรือ `PATCH /api/v1/admin/users/9999/suspend` + Token แอดมิน | คืนค่า Error `"Report not found"` หรือ `"User not found"` | `404 Not Found` |
| **TC-US10-12** | Validation Error: ส่งค่า `decision` ผิด, ไม่ส่ง `suspended`, `adminNote` เกิน 1000 ตัวอักษร หรือ ID ผิดรูปแบบ | `PATCH /api/v1/admin/reports/12/decision` + `{ "decision": "INVALID" }` (หรือ `{}` ใน `/suspend`) | คืนค่า `"Validation failed"` พร้อมรายละเอียด field error | `400 Bad Request` |