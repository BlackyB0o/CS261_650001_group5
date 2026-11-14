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