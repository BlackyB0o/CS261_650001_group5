# Task 38: US05 - Sequence Diagram และ UI หน้ารายละเอียดออเดอร์

## 1. Sequence Diagram

### 1.1 Get Order Detail & Status History (GET /api/v1/orders/{orderId})

```mermaid
sequenceDiagram
    autonumber
    actor Buyer as ผู้ฝากหิ้ว (Buyer)
    participant FE as Frontend App
    participant Auth as Security Filter / JWT Validator
    participant API as OrderController
    participant SVC as OrderServiceImpl
    participant OrderRepo as OrderRepository
    participant HistoryRepo as OrderStatusHistoryRepository
    participant DB as Database

    Buyer->>FE: 1. กดเลือกรายการฝากซื้อเพื่อดูรายละเอียดและสถานะ (orderId)
    FE->>Auth: 2. GET /api/v1/orders/{orderId}<br/>Header: Authorization: Bearer <Token>
    activate Auth
    
    Auth->>Auth: 3. ตรวจสอบความถูกต้องของ JWT Token & ถอดรหัสหา buyerId
    
    alt กรณี Token ไม่ถูกต้อง หรือหมดอายุ
        Auth-->>FE: HTTP 401 Unauthorized (Invalid / Expired Token)
        FE-->>Buyer: แสดงแจ้งเตือน Token หมดอายุ / Redirect ไปหน้า Login
    else กรณี Token ถูกต้อง
        Auth->>API: 4. ส่งต่อ Request พร้อม User Principal (buyerId, orderId)
        deactivate Auth
        activate API
        
        API->>SVC: 5. getOrderDetail(buyerId, orderId)
        activate SVC
        
        SVC->>OrderRepo: 6. findByIdWithDetails(orderId)
        activate OrderRepo
        OrderRepo->>DB: 7. SELECT o.*, i.*, r.* FROM orders o JOIN order_items i ... JOIN delivery_rounds r ... WHERE o.id = orderId
        activate DB
        DB-->>OrderRepo: 8. Return Optional<Order>
        deactivate DB
        OrderRepo-->>SVC: 9. Return Optional<Order>
        deactivate OrderRepo
        
        alt กรณีไม่พบรายการฝากซื้อในระบบ
            SVC-->>API: Throw ApiException.notFound ("Order not found")
            API-->>FE: HTTP 404 Not Found { "message": "Order not found" }
            FE-->>Buyer: แสดงแจ้งเตือน "ไม่พบรายการฝากซื้อ"
        else กรณีพบรายการฝากซื้อ
            SVC->>SVC: 10. ตรวจสอบสิทธิ์เจ้าของออเดอร์ (order.getBuyerId() == buyerId)
            
            alt กรณีไม่ใช่เจ้าของรายการฝากซื้อ
                SVC-->>API: Throw AccessDeniedException ("Access denied")
                API-->>FE: HTTP 403 Forbidden { "message": "ไม่สามารถดูรายละเอียดออเดอร์ของผู้อื่นได้" }
                FE-->>Buyer: แสดงแจ้งเตือน "ไม่มีสิทธิ์เข้าถึงรายการฝากซื้อนี้"
            else กรณีเป็นเจ้าของรายการฝากซื้อถูกต้อง (Test Case 1 & 2)
                SVC->>HistoryRepo: 11. findAllByOrderIdOrderByCreatedAtAsc(orderId)
                activate HistoryRepo
                HistoryRepo->>DB: 12. SELECT * FROM order_status_histories WHERE order_id = orderId ORDER BY created_at ASC
                activate DB
                DB-->>HistoryRepo: 13. Return List<OrderStatusHistory>
                deactivate DB
                HistoryRepo-->>SVC: 14. Return ประวัติสถานะและสถานะล่าสุด
                deactivate HistoryRepo
                
                SVC->>SVC: 15. Map Entity -> OrderDetailResponseDTO (รายการสินค้า, จุดนัดรับ, สถานะล่าสุด, ประวัติสถานะ)
                SVC-->>API: 16. Return OrderDetailResponseDTO
                deactivate SVC
                
                API-->>FE: 17. HTTP 200 OK (JSON Payload)
                deactivate API
                FE-->>Buyer: 18. แสดงรายละเอียดสินค้า จุดนัดรับ สถานะล่าสุด และ Timeline ความคืบหน้า
            end
        end
    end
