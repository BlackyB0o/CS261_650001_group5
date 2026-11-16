# Task 33: US04 - Sequence Diagram และ UI แสดงรายการฝากซื้อในรอบ

## 1. Sequence Diagram

### 1.1 View Round Orders (GET /api/v1/rounds/{roundId}/orders)

```mermaid
sequenceDiagram
    autonumber
    actor Shopper as ผู้รับหิ้ว (Shopper)
    participant FE as Frontend App
    participant Auth as Security Filter / JWT Validator
    participant API as OrderController
    participant SVC as OrderServiceImpl
    participant RoundRepo as RoundRepository
    participant OrderRepo as OrderRepository
    participant DB as Database

    Shopper->>FE: 1. กดเลือกดูรายการฝากซื้อในรอบที่เปิดอยู่ (roundId)
    FE->>Auth: 2. GET /api/v1/rounds/{roundId}/orders<br/>Header: Authorization: Bearer <Token>
    activate Auth
    
    Auth->>Auth: 3. ตรวจสอบความถูกต้องของ JWT Token & ถอดรหัสหา shopperId
    
    alt กรณี Token ไม่ถูกต้อง หรือหมดอายุ
        Auth-->>FE: HTTP 401 Unauthorized (Invalid / Expired Token)
        FE-->>Shopper: แสดงแจ้งเตือน Token หมดอายุ / Redirect ไป Login
    else กรณี Token ถูกต้อง
        Auth->>API: 4. ส่งต่อ Request พร้อม User Principal (shopperId, roundId)
        deactivate Auth
        activate API
        
        API->>SVC: 5. getOrdersByRound(shopperId, roundId)
        activate SVC
        
        SVC->>RoundRepo: 6. findById(roundId)
        activate RoundRepo
        RoundRepo->>DB: 7. SELECT * FROM rounds WHERE id = roundId
        activate DB
        DB-->>RoundRepo: 8. Return Round Entity
        deactivate DB
        RoundRepo-->>SVC: 9. Return Optional<Round>
        deactivate RoundRepo
        
        SVC->>SVC: 10. ตรวจสอบสิทธิ์เจ้าของรอบ (round.getShopperId() == shopperId)
        
        alt กรณีไม่ใช่รอบของผู้รับหิ้วคนนี้ (Test Case 2)
            SVC-->>API: Throw AccessDeniedException ("Unauthorized access to round")
            API-->>FE: HTTP 403 Forbidden { "message": "ไม่สามารถดูรายการฝากซื้อของรอบอื่นได้" }
            FE-->>Shopper: แสดงแจ้งเตือน "ไม่สามารถดูรายการฝากซื้อของรอบอื่นได้"
        else กรณีเป็นรอบของผู้รับหิ้วถูกต้อง (Test Case 1)
            SVC->>OrderRepo: 11. findAllByRoundIdWithItems(roundId)
            activate OrderRepo
            OrderRepo->>DB: 12. SELECT o.*, i.*, u.name FROM orders o JOIN order_items i ON o.id = i.order_id JOIN users u ON o.buyer_id = u.id WHERE o.round_id = roundId
            activate DB
            DB-->>OrderRepo: 13. Return List<Order> พร้อม OrderItems และข้อมูลผู้ฝาก
            deactivate DB
            OrderRepo-->>SVC: 14. Return Order List Entities
            deactivate OrderRepo
            
            SVC->>SVC: 15. Map Entities -> List<OrderSummaryResponseDto> (รวมรายการสินค้าและแยกตามผู้ฝาก)
            SVC-->>API: 16. Return List<OrderSummaryResponseDto>
            deactivate SVC
            
            API-->>FE: 17. HTTP 200 OK (JSON Payload)
            deactivate API
            FE-->>Shopper: 18. แสดงรายการฝากซื้อทั้งหมดในรอบ ให้ผู้รับหิ้วตรวจสอบก่อนเดินทางไปซื้อ
        end
    end
```

## 2. Test Cases 

| Test Case ID | หัวข้อการทดสอบ (Test Scenario) | เงื่อนไข / Input Data | ผลลัพธ์ที่คาดหวัง (Expected Output) | HTTP Status |
| :--- | :--- | :--- | :--- | :---: |
| **TC-US04-01** | ดูรายการฝากซื้อในรอบของตนเองสำเร็จ (มีข้อมูลสั่งซื้อ) | `GET /api/v1/rounds/101/orders` + Token เจ้าของรอบ | คืนค่า List รายการสั่งซื้อทั้งหมดพร้อมรายละเอียดสินค้าและข้อมูลผู้ฝาก | `200 OK` |
| **TC-US04-02** | ดูรายการฝากซื้อในรอบของตนเอง แต่ยังไม่มีใครฝากซื้อ (Empty List) | `GET /api/v1/rounds/102/orders` + Token เจ้าของรอบ | คืนค่า `data: []` (อาร์เรย์ว่าง) ไม่เกิด Error | `200 OK` |
| **TC-US04-03** | ป้องกันผู้รับหิ้วคนอื่นแอบดูรายการฝากซื้อในรอบ | `GET /api/v1/rounds/101/orders` + Token ผู้รับหิ้วท่านอื่น | ปฏิเสธการเข้าถึง และคืนค่า Error `"ไม่สามารถดูรายการฝากซื้อของรอบอื่นได้"` | `403 Forbidden` |
| **TC-US04-04** | ป้องกันผู้ฝากหิ้ว (Buyer/Role อื่น) เรียกดู API ผู้รับหิ้ว | `GET /api/v1/rounds/101/orders` + Token ของผู้ฝากหิ้ว | ปฏิเสธการเข้าถึง เนื่องจากสิทธิ์ไม่ถึง (Access Denied) | `403 Forbidden` |
| **TC-US04-05** | ผู้ใช้งานไม่ได้ยืนยันตัวตน / Token หมดอายุ | `GET /api/v1/rounds/101/orders` (ไม่ส่ง Header Authorization) | คืนค่า Error Unauthorized | `401 Unauthorized` |
| **TC-US04-06** | ระบุ ID รอบรับหิ้วที่ไม่พบในระบบ | `GET /api/v1/rounds/9999/orders` + Token เจ้าของรอบ | คืนค่า Error Message `"Round not found"` | `404 Not Found` |
| **TC-US04-07** | Validation Error: รูปแบบ `roundId` ไม่ถูกต้อง | `GET /api/v1/rounds/abc/orders` + Token เจ้าของรอบ | คืนค่า Validation Error สำหรับ Parameter `roundId` ต้องเป็นตัวเลข | `400 Bad Request` |