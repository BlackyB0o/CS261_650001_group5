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
