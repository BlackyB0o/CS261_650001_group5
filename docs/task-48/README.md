# Task 48: US09 - Sequence Diagram และ UI ฟอร์มรายงานปัญหา

## 1. Sequence Diagram

### 1.1 Submit Issue / Suspicious Behavior Report (POST /api/v1/reports)

```mermaid
sequenceDiagram
    autonumber
    actor User as ผู้ใช้งาน (ผู้ฝากหิ้ว / ผู้รับหิ้ว)
    participant FE as Frontend App (UI Form)
    participant Auth as JWT Auth Filter & Validator
    participant Controller as ReportController
    participant SVC as ReportServiceImpl
    participant OrderRepo as OrderRepository
    participant ReportRepo as ReportRepository
    participant DB as Database

    User->>FE: 1. กดปุ่มรายงานปัญหา และกรอกฟอร์ม (orderId, category, description, evidenceUrl)
    FE->>Auth: 2. POST /api/v1/reports<br/>Header: Authorization: Bearer JWT_TOKEN<br/>Body: { "orderId": 501, "category": "FRAUD", "description": "..." }
    activate Auth

    Auth->>Auth: 3. ตรวจสอบ JWT Token และ @Valid Request Body

    alt กรณีไม่มี Token หรือ Token ไม่ถูกต้อง/หมดอายุ
        Auth-->>FE: HTTP 401 Unauthorized ("Invalid or missing token")
        FE-->>User: แสดงแจ้งเตือน และ Redirect ไปหน้า Login
    else กรณีไม่กรอกรายละเอียดปัญหา (description ว่าง) หรือข้อมูลไม่ครบ (Test Case 2)
        Auth-->>FE: HTTP 400 Bad Request ("Validation failed")
        FE-->>User: แสดงแจ้งเตือน "กรุณากรอกรายละเอียดของปัญหา" (ไม่สามารถส่งรายงานได้)
    else กรณี Token และรูปแบบข้อมูลถูกต้อง
        Auth->>Controller: 4. createReport(userId, CreateReportRequestDTO)
        deactivate Auth
        activate Controller

        Controller->>SVC: 5. createReport(reporterId, request)
        activate SVC

        SVC->>OrderRepo: 6. findById(orderId)
        activate OrderRepo
        OrderRepo->>DB: 7. SELECT * FROM orders WHERE id = :orderId
        activate DB
        DB-->>OrderRepo: 8. Return Optional<Order>
        deactivate DB
        OrderRepo-->>SVC: 9. Return Optional<Order>
        deactivate OrderRepo

        alt กรณีไม่พบรายการฝากซื้อที่อ้างอิงในระบบ
            SVC-->>Controller: Throw ApiException.notFound("Order not found")
            Controller-->>FE: HTTP 404 Not Found ("Order not found")
            FE-->>User: แสดงแจ้งเตือน "ไม่พบรายการฝากซื้อที่ต้องการรายงาน"
        else กรณีพบรายการฝากซื้อ
            SVC->>SVC: 10. ตรวจสอบสิทธิ์คู่กรณี (reporterId ต้องเป็น buyer หรือ runner ของออเดอร์นี้)

            alt กรณีผู้แจ้งไม่ใช่คู่กรณีในออเดอร์นี้
                SVC-->>Controller: Throw ApiException.forbidden("You are not involved in this order")
                Controller-->>FE: HTTP 403 Forbidden ("You are not involved in this order")
                FE-->>User: แสดงแจ้งเตือน "ไม่มีสิทธิ์รายงานปัญหาในออเดอร์นี้"
            else กรณีเป็นคู่กรณีถูกต้อง (Test Case 1)
                SVC->>SVC: 11. ระบุคู่กรณีที่ถูกรายงาน (reportedUserId) และกำหนดสถานะเริ่มต้น PENDING
                SVC->>ReportRepo: 12. save(Report Entity)
                activate ReportRepo
                ReportRepo->>DB: 13. INSERT INTO reports (order_id, reporter_id, reported_user_id, category, description, evidence_url, status, created_at)
                activate DB
                DB-->>ReportRepo: 14. Return Saved Report
                deactivate DB
                ReportRepo-->>SVC: 15. Return Report Entity
                deactivate ReportRepo

                SVC->>SVC: 16. Map Entity -> ReportResponseDTO
                SVC-->>Controller: 17. Return ReportResponseDTO
                deactivate SVC

                Controller-->>FE: 18. HTTP 201 Created (ApiResponseDTO.success("Report submitted successfully", data))
                deactivate Controller
                FE-->>User: 19. ปิดฟอร์มและแสดงแจ้งเตือน "ส่งรายงานปัญหาเรียบร้อยแล้ว"
            end
        end
    end
```
---

## 2. Test Cases (US09)

| Test Case ID | หัวข้อการทดสอบ (Test Scenario) | เงื่อนไข / Input Data | ผลลัพธ์ที่คาดหวัง (Expected Output) | HTTP Status |
| :--- | :--- | :--- | :--- | :---: |
| **TC-US09-01** | ผู้ฝากหิ้วส่งรายงานปัญหาหรือพฤติกรรมน่าสงสัยของคู่กรณีพร้อมรายละเอียดสำเร็จ (Test Case 1) | `POST /api/v1/reports` + Token ผู้ฝากหิ้ว + `{ "orderId": 501, "category": "NOT_DELIVERED", "description": "ผู้รับหิ้วไม่นำสินค้ามาส่งตามเวลานัด" }` | บันทึกข้อมูลลงตาราง `reports` สถานะ `PENDING` และคืนข้อความ `"Report submitted successfully"` | `201 Created` |
| **TC-US09-02** | ผู้รับหิ้วส่งรายงานปัญหาหรือพฤติกรรมน่าสงสัยของคู่กรณีพร้อมรายละเอียดสำเร็จ (Test Case 1) | `POST /api/v1/reports` + Token ผู้รับหิ้ว + `{ "orderId": 501, "category": "SUSPICIOUS_BEHAVIOR", "description": "ผู้ฝากหิ้วให้ข้อมูลจุดนัดรับเท็จและติดต่อไม่ได้" }` | บันทึกข้อมูลลงตาราง `reports` สถานะ `PENDING` และคืนข้อความ `"Report submitted successfully"` | `201 Created` |
| **TC-US09-03** | ระบบไม่สามารถส่งรายงานได้ หากผู้ใช้ไม่กรอกรายละเอียดของปัญหา (Test Case 2) | `POST /api/v1/reports` + Token ผู้ใช้งาน + `{ "orderId": 501, "category": "FRAUD", "description": "" }` (หรือเป็น `null`) | ปฏิเสธการส่งรายงาน และคืนค่า `"Validation failed"` พร้อมแจ้งเตือนฟิลด์ `description` ห้ามว่าง | `400 Bad Request` |
| **TC-US09-04** | Validation Error: ไม่ระบุประเภทปัญหา (`category`) หรือไม่ระบุ `orderId` | `POST /api/v1/reports` + Token ผู้ใช้งาน + `{ "description": "พบปัญหาการใช้งาน" }` | ปฏิเสธการส่งรายงาน และคืนค่า `"Validation failed"` พร้อมรายละเอียด field error | `400 Bad Request` |
| **TC-US09-05** | ป้องกันบุคคลภายนอกที่ไม่ได้เป็นคู่กรณีในออเดอร์นั้นส่งรายงาน | `POST /api/v1/reports` + Token ผู้ใช้งานอื่นที่ไม่ใช่ทั้งผู้ฝากและผู้รับหิ้วของออเดอร์ 501 | ปฏิเสธการเข้าถึง คืนค่า Error `"You are not involved in this order"` | `403 Forbidden` |
| **TC-US09-06** | ระบุรหัสออเดอร์ (`orderId`) ที่ไม่มีอยู่จริงในระบบ | `POST /api/v1/reports` + Token ผู้ใช้งาน + `{ "orderId": 9999, "category": "FRAUD", "description": "แจ้งปัญหาออเดอร์" }` | คืนค่า Error `"Order not found"` | `404 Not Found` |
| **TC-US09-07** | ผู้ใช้งานไม่ได้ยืนยันตัวตน / ไม่ส่ง Token หรือ Token หมดอายุ | `POST /api/v1/reports` (ไม่ส่ง Header Authorization) | คืนค่า Error `"Invalid or missing token"` | `401 Unauthorized` |