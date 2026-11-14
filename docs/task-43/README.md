# Task 43: US06 - Sequence Diagram (State Transition) และ UI เปลี่ยนสถานะ

## 1. Sequence Diagram

### 1.1 Update Order Status & State Transition (PATCH /api/v1/orders/{orderId}/status)

```mermaid
sequenceDiagram
    autonumber
    actor Runner as ผู้รับหิ้ว (Runner)
    participant FE as Frontend App (UI Modal)
    participant Auth as Security Filter / JWT Validator
    participant API as OrderController
    participant SVC as OrderServiceImpl
    participant OrderRepo as OrderRepository
    participant HistoryRepo as OrderStatusHistoryRepository
    participant DB as Database

    Runner->>FE: 1. เลือกเปลี่ยนสถานะออเดอร์ใน UI Modal (orderId, newStatus)
    FE->>Auth: 2. PATCH /api/v1/orders/{orderId}/status<br/>Header: Authorization: Bearer <Token><br/>Body: { "status": "BUYING" }
    activate Auth
    
    Auth->>Auth: 3. ตรวจสอบความถูกต้องของ JWT Token & ถอดรหัสหา runnerId
    
    alt กรณี Token ไม่ถูกต้อง หรือหมดอายุ
        Auth-->>FE: HTTP 401 Unauthorized (Invalid / Expired Token)
        FE-->>Runner: แสดงแจ้งเตือน Token หมดอายุ / Redirect ไปหน้า Login
    else กรณี Token ถูกต้อง
        Auth->>API: 4. ส่งต่อ Request พร้อม User Principal (runnerId, orderId, newStatus)
        deactivate Auth
        activate API
        
        API->>SVC: 5. updateOrderStatus(runnerId, orderId, newStatus)
        activate SVC
        
        SVC->>OrderRepo: 6. findByIdWithRound(orderId)
        activate OrderRepo
        OrderRepo->>DB: 7. SELECT o.*, r.* FROM orders o JOIN delivery_rounds r ON o.round_id = r.id WHERE o.id = orderId
        activate DB
        DB-->>OrderRepo: 8. Return Optional<Order>
        deactivate DB
        OrderRepo-->>SVC: 9. Return Optional<Order>
        deactivate OrderRepo
        
        alt กรณีไม่พบรายการฝากซื้อในระบบ
            SVC-->>API: Throw ApiException.notFound ("Order not found")
            API-->>FE: HTTP 404 Not Found { "message": "Order not found" }
            FE-->>Runner: แสดงแจ้งเตือน "ไม่พบรายการฝากซื้อ"
        else กรณีพบรายการฝากซื้อ
            SVC->>SVC: 10. ตรวจสอบสิทธิ์เจ้าของรอบรับหิ้ว (order.getRound().getRunnerId() == runnerId)
            
            alt กรณีไม่ใช่ผู้รับหิ้วที่ดูแลรอบนี้
                SVC-->>API: Throw AccessDeniedException ("Access denied")
                API-->>FE: HTTP 403 Forbidden { "message": "ไม่มีสิทธิ์อัปเดตสถานะออเดอร์ของรอบอื่น" }
                FE-->>Runner: แสดงแจ้งเตือน "ไม่มีสิทธิ์ดำเนินการในออเดอร์นี้"
            else กรณีเป็นผู้รับหิ้วเจ้าของรอบถูกต้อง
                SVC->>SVC: 11. ตรวจสอบเงื่อนไข State Transition<br/>(เช่น PAID -> BUYING -> DELIVERING -> READY_FOR_PICKUP)
                
                alt กรณีเปลี่ยนสถานะข้ามขั้นตอน หรือย้อนกลับผิดเงื่อนไข (Invalid State Transition)
                    SVC-->>API: Throw ApiException.badRequest ("Invalid status transition")
                    API-->>FE: HTTP 400 Bad Request { "message": "ไม่สามารถเปลี่ยนสถานะจาก {current} เป็น {newStatus} ได้" }
                    FE-->>Runner: แสดงแจ้งเตือน "ลำดับขั้นตอนการเปลี่ยนสถานะไม่ถูกต้อง"
                else กรณีลำดับสถานะถูกต้อง (Test Case 1 & 2)
                    SVC->>OrderRepo: 12. อัปเดตสถานะล่าสุด (order.setStatus(newStatus)) & save(order)
                    activate OrderRepo
                    OrderRepo->>DB: 13. UPDATE orders SET status = :newStatus, updated_at = NOW() WHERE id = orderId
                    activate DB
                    DB-->>OrderRepo: 14. Return Updated Order
                    deactivate DB
                    OrderRepo-->>SVC: 15. Return Order Entity
                    deactivate OrderRepo
                    
                    SVC->>HistoryRepo: 16. บันทึกประวัติการเปลี่ยนสถานะ save(OrderStatusHistory)
                    activate HistoryRepo
                    HistoryRepo->>DB: 17. INSERT INTO order_status_histories (order_id, status, changed_by, created_at)
                    activate DB
                    DB-->>HistoryRepo: 18. Return Saved History
                    deactivate DB
                    HistoryRepo-->>SVC: 19. Return OrderStatusHistory
                    deactivate HistoryRepo
                    
                    SVC->>SVC: 20. Map Entity -> OrderStatusResponseDTO
                    SVC-->>API: 21. Return OrderStatusResponseDTO
                    deactivate SVC
                    
                    API-->>FE: 22. HTTP 200 OK (JSON Payload พร้อมสถานะล่าสุด)
                    deactivate API
                    FE-->>Runner: 23. ปิด Modal และอัปเดตสถานะล่าสุดบนหน้าจอ (ผู้ฝากหิ้วเห็นสถานะใหม่ทันที)
                end
            end
        end
    end