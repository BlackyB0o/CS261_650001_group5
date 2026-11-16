# Task 43: US06 - Sequence Diagram (State Transition) และ UI เปลี่ยนสถานะ

## 1. Sequence Diagram

### 1.1 Update Order Status & State Transition (PATCH /api/v1/orders/{orderId}/status)

```mermaid
sequenceDiagram
    autonumber
    actor Runner as ผู้รับหิ้ว (Runner)
    participant FE as Frontend App (UI Modal)
    participant Auth as JWT Auth Filter & Validator
    participant Controller as OrderController
    participant SVC as OrderServiceImpl
    participant Rules as OrderStatusRules
    participant OrderRepo as OrderRepository
    participant HistoryRepo as OrderStatusHistoryRepository
    participant DB as Database

    Runner->>FE: 1. กดเลือกเปลี่ยนสถานะออเดอร์ใน UI Modal (orderId, newStatus)
    FE->>Auth: 2. PATCH /api/v1/orders/{orderId}/status<br/>Header: Authorization: Bearer JWT_TOKEN<br/>Body: { "status": "PURCHASING" }
    activate Auth

    Auth->>Auth: 3. ตรวจสอบ JWT Token และ @Valid Request Body / PathVariable

    alt กรณีไม่มี Token หรือ Token ไม่ถูกต้อง/หมดอายุ
        Auth-->>FE: HTTP 401 Unauthorized ("Invalid or missing token")
        FE-->>Runner: แสดงแจ้งเตือน และ Redirect ไปหน้า Login
    else กรณีรูปแบบข้อมูลผิด (status ไม่อยู่ในระบบ/ค่าว่าง หรือ orderId ไม่ใช่ตัวเลข)
        Auth-->>FE: HTTP 400 Bad Request ("Validation failed")
        FE-->>Runner: แสดงแจ้งเตือนข้อมูลไม่ถูกต้อง
    else กรณี Token และรูปแบบข้อมูลถูกต้อง
        Auth->>Controller: 4. updateStatus(userId, orderId, UpdateOrderStatusRequestDTO)
        deactivate Auth
        activate Controller

        Controller->>SVC: 5. updateStatus(runnerId, orderId, request)
        activate SVC

        SVC->>OrderRepo: 6. findById(orderId)
        activate OrderRepo
        OrderRepo->>DB: 7. SELECT * FROM orders WHERE id = :orderId
        activate DB
        DB-->>OrderRepo: 8. Return Optional<Order>
        deactivate DB
        OrderRepo-->>SVC: 9. Return Optional<Order>
        deactivate OrderRepo

        alt กรณีไม่พบออเดอร์ในระบบ
            SVC-->>Controller: Throw ApiException.notFound("Order not found")
            Controller-->>FE: HTTP 404 Not Found ("Order not found")
            FE-->>Runner: แสดงแจ้งเตือน "ไม่พบรายการฝากซื้อ"
        else กรณีพบออเดอร์
            SVC->>SVC: 10. ตรวจสอบสิทธิ์เจ้าของรอบ (order.round.runner.id == runnerId)

            alt กรณีไม่ใช่ผู้รับหิ้วเจ้าของรอบ (รวมถึงผู้ฝากหิ้วของออเดอร์)
                SVC-->>Controller: Throw ApiException.forbidden("You are not the owner of this delivery round")
                Controller-->>FE: HTTP 403 Forbidden ("You are not the owner of this delivery round")
                FE-->>Runner: แสดงแจ้งเตือน "ไม่มีสิทธิ์อัปเดตสถานะออเดอร์นี้"
            else กรณีเป็นผู้รับหิ้วเจ้าของรอบถูกต้อง
                SVC->>Rules: 11. canRunnerChange(oldStatus, newStatus)
                activate Rules
                Rules-->>SVC: 12. Return boolean (ตรวจตามกติกา State Machine ทีละขั้น)
                deactivate Rules

                alt กรณีเปลี่ยนข้ามขั้น, ย้อนกลับ, สถานะเดิม หรือเปลี่ยนเป็น PAID/COMPLETED เอง
                    SVC-->>Controller: Throw ApiException.conflict("Cannot change status from {old} to {new}")
                    Controller-->>FE: HTTP 409 Conflict ("Cannot change status from {old} to {new}")
                    FE-->>Runner: แสดงแจ้งเตือน "ไม่สามารถเปลี่ยนสถานะข้ามขั้นตอนหรือย้อนกลับได้"
                else กรณีลำดับสถานะถูกต้อง (@Transactional)
                    SVC->>OrderRepo: 13. ตั้งค่า order.setStatus(newStatus) & save(order)
                    activate OrderRepo
                    OrderRepo->>DB: 14. UPDATE orders SET status = :newStatus WHERE id = :orderId
                    activate DB
                    DB-->>OrderRepo: 15. Return Updated Order
                    deactivate DB
                    OrderRepo-->>SVC: 16. Return Order
                    deactivate OrderRepo

                    SVC->>HistoryRepo: 17. save(OrderStatusHistory.of(order, oldStatus, newStatus, runner))
                    activate HistoryRepo
                    HistoryRepo->>DB: 18. INSERT INTO order_status_history (order_id, from_status, to_status, changed_by, changed_at)
                    activate DB
                    DB-->>HistoryRepo: 19. Return Saved OrderStatusHistory
                    deactivate DB
                    HistoryRepo-->>SVC: 20. Return OrderStatusHistory
                    deactivate HistoryRepo

                    SVC-->>Controller: 21. Return OrderStatusResponseDTO(orderId, status)
                    deactivate SVC

                    Controller-->>FE: 22. HTTP 200 OK (ApiResponseDTO.success("Order status updated successfully", data))
                    deactivate Controller
                    FE-->>Runner: 23. ปิด Modal และอัปเดตสถานะล่าสุดบนหน้าจอ (ผู้ฝากหิ้วเห็นสถานะใหม่ทันที)
                end
            end
        end
    end
```
## 2. Test Cases (US06)

| Test Case ID | หัวข้อการทดสอบ (Test Scenario) | เงื่อนไข / Input Data | ผลลัพธ์ที่คาดหวัง (Expected Output) | HTTP Status |
| :--- | :--- | :--- | :--- | :---: |
| **TC-US06-01** | ผู้รับหิ้วรับออเดอร์หลังชำระเงินสำเร็จ (`PAID` → `ACCEPTED`) | `PATCH /api/v1/orders/501/status` + Token เจ้าของรอบ + `{ "status": "ACCEPTED" }` | อัปเดตสถานะเป็น `ACCEPTED` บันทึกประวัติลง `order_status_history` และคืน `"Order status updated successfully"` | `200 OK` |
| **TC-US06-02** | ผู้รับหิ้วอัปเดตสถานะตามลำดับจนถึงส่งมอบ (`ACCEPTED` → `PURCHASING` → `READY_FOR_DELIVERY` → `DELIVERING`) | `PATCH /api/v1/orders/501/status` + Token เจ้าของรอบ (อัปเดตทีละขั้น) | บันทึกสถานะล่าสุดสำเร็จทุกขั้น และผู้ฝากหิ้วเห็นสถานะล่าสุดตรงกันเมื่อเรียก `GET /api/v1/orders/501` | `200 OK` |
| **TC-US06-03** | ผู้รับหิ้วยกเลิกออเดอร์ที่ยังไม่ชำระเงิน (`PENDING` → `CANCELLED`) | `PATCH /api/v1/orders/502/status` + Token เจ้าของรอบ + `{ "status": "CANCELLED" }` | อัปเดตสถานะเป็น `CANCELLED` และบันทึกประวัติ `PENDING -> CANCELLED` สำเร็จ | `200 OK` |
| **TC-US06-04** | ป้องกันการอัปเดตสถานะข้ามขั้นตอน (`PAID` → `DELIVERING`) | `PATCH /api/v1/orders/501/status` (สถานะเดิม `PAID`) + `{ "status": "DELIVERING" }` | ปฏิเสธการเปลี่ยนสถานะ คืนค่า `"Cannot change status from PAID to DELIVERING"` | `409 Conflict` |
| **TC-US06-05** | ป้องกันการย้อนกลับสถานะ หรือเปลี่ยนเป็นสถานะเดิมซ้ำ | `PATCH /api/v1/orders/501/status` (สถานะเดิม `PURCHASING`) + `{ "status": "ACCEPTED" }` | ปฏิเสธการเปลี่ยนสถานะ คืนค่า `"Cannot change status from PURCHASING to ACCEPTED"` | `409 Conflict` |
| **TC-US06-06** | ป้องกันผู้รับหิ้วเปลี่ยนเป็น `PAID`/`COMPLETED` เอง หรือแก้ไขออเดอร์ที่จบแล้ว | `PATCH /api/v1/orders/501/status` + `{ "status": "COMPLETED" }` (หรือแก้จาก `COMPLETED`/`CANCELLED`) | ปฏิเสธการเปลี่ยนสถานะเนื่องจากสงวนสิทธิ์ให้ผู้ฝากหิ้ว หรือออเดอร์ปิดงานแล้ว | `409 Conflict` |
| **TC-US06-07** | ป้องกันผู้รับหิ้วคนอื่น หรือผู้ฝากหิ้วแอบเปลี่ยนสถานะออเดอร์ | `PATCH /api/v1/orders/501/status` + Token ผู้รับหิ้วรอบอื่น (หรือผู้ฝากหิ้ว) + `{ "status": "ACCEPTED" }` | ปฏิเสธการเข้าถึง คืนค่า `"You are not the owner of this delivery round"` | `403 Forbidden` |
| **TC-US06-08** | ระบุ ID รายการฝากซื้อ (`orderId`) ที่ไม่พบในระบบ | `PATCH /api/v1/orders/9999/status` + Token เจ้าของรอบ + `{ "status": "ACCEPTED" }` | คืนค่า Error `"Order not found"` | `404 Not Found` |
| **TC-US06-09** | ผู้ใช้งานไม่ได้ยืนยันตัวตน / Token หมดอายุ | `PATCH /api/v1/orders/501/status` (ไม่ส่ง Header Authorization) | คืนค่า Error `"Invalid or missing token"` | `401 Unauthorized` |
| **TC-US06-10** | Validation Error: ส่งค่า `status` ผิด/ค่าว่าง หรือ `orderId` ไม่ใช่ตัวเลข | `PATCH /api/v1/orders/501/status` + `{ "status": "IN_PROGRESS" }` (หรือ `orderId = abc`) | คืนค่า `"Validation failed"` พร้อมรายละเอียด field error | `400 Bad Request` |