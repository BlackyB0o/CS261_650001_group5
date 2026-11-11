# Task 28: US02 - Sequence Diagram และ UI Prototype สำหรับระบบค้นหารอบรับหิ้ว

## 1. Sequence Diagram

### 1.1 Search Delivery Rounds (GET /api/v1/delivery-rounds)

```mermaid
sequenceDiagram
    autonumber
    actor Guest as ผู้ฝากหิ้ว / Guest
    participant FE as Frontend App
    participant Controller as DeliveryRoundController
    participant SVC as DeliveryRoundServiceImpl
    participant Repo as DeliveryRoundRepository
    participant DB as Database

    Guest->>FE: 1. ป้อนคำค้นหา (keyword, date, status, page, size)
    FE->>Controller: 2. GET /api/v1/delivery-rounds?keyword=...&date=...&page=1&size=20
    activate Controller
    
    Controller->>SVC: 3. getRounds(RoundSearchRequestDTO criteria)
    activate SVC
    
    SVC->>SVC: 4. ตรวจสอบและเตรียมข้อมูล:<br/>- กำหนด status Default = "OPEN" (หากไม่ได้ระบุ)<br/>- Escape ตัวอักษร '%', '_' ใน keyword และจัดรูปแบบเป็น %keyword%<br/>- สร้าง Pageable จาก page - 1 และ size
    
    alt กรณี date ไม่ถูกต้อง (เช่น วันที่ไม่จริง หรือรูปแบบผิด)
        SVC-->>Controller: Throw ApiException.validation ("date must be a valid date in yyyy-MM-dd format")
        Controller-->>FE: HTTP 400 Bad Request (Validation failed)
        FE-->>Guest: แสดงแจ้งเตือน "รูปแบบวันที่ไม่ถูกต้อง"
    else กรณีข้อมูลถูกต้อง
        alt มีการระบุ date
            SVC->>Repo: 5a. searchByDate(status, keyword, startOfDay, endOfDay, pageable)
            activate Repo
            Repo->>DB: 6a. SELECT r FROM DeliveryRound r WHERE ... AND r.scheduledAt BETWEEN :from AND :to
            activate DB
            DB-->>Repo: 7a. Return Page<DeliveryRound>
            deactivate DB
            Repo-->>SVC: 8a. Return Page<DeliveryRound>
            deactivate Repo
        else ไม่ได้ระบุ date
            SVC->>Repo: 5b. search(status, keyword, pageable)
            activate Repo
            Repo->>DB: 6b. SELECT r FROM DeliveryRound r WHERE r.status = :status AND (LOWER(storeName) LIKE ... OR LOWER(locationName) LIKE ... OR LOWER(runner.fullName) LIKE ...)
            activate DB
            DB-->>Repo: 7b. Return Page<DeliveryRound>
            deactivate DB
            Repo-->>SVC: 8b. Return Page<DeliveryRound>
            deactivate Repo
        end

        SVC->>SVC: 9. แปลง Page<DeliveryRound> -> Page<DeliveryRoundResponseDTO>
        SVC-->>Controller: 10. Return Page<DeliveryRoundResponseDTO>
        deactivate SVC
        
        Controller->>Controller: 11. ประกอบ ApiResponseDTO (data + PageMetaDTO)
        Controller-->>FE: 12. HTTP 200 OK (JSON Payload พร้อม pagination)
        deactivate Controller
        FE-->>Guest: 13. แสดงผลรายการรอบรับหิ้วและ Pagination
    end
```
### 1.2 Get Delivery Round Detail (GET /api/v1/delivery-rounds/{roundId})
```mermaid
sequenceDiagram
    autonumber
    actor Guest as ผู้ฝากหิ้ว / Guest
    participant FE as Frontend App
    participant Controller as DeliveryRoundController
    participant SVC as DeliveryRoundServiceImpl
    participant Repo as DeliveryRoundRepository
    participant DB as Database

    Guest->>FE: 1. กดเลือกดูรายละเอียดรอบรับหิ้ว (roundId)
    FE->>Controller: 2. GET /api/v1/delivery-rounds/{roundId}
    activate Controller
    
    Controller->>SVC: 3. getRound(roundId)
    activate SVC
    
    SVC->>Repo: 4. findById(roundId)
    activate Repo
    Repo->>DB: 5. SELECT * FROM delivery_rounds WHERE id = roundId
    activate DB
    DB-->>Repo: 6. Return Optional<DeliveryRound>
    deactivate DB
    Repo-->>SVC: 7. Return Optional<DeliveryRound>
    deactivate Repo
    
    alt ไม่พบข้อมูลรอบรับหิ้ว (Optional.isEmpty)
        SVC-->>Controller: Throw ApiException.notFound ("Delivery round not found")
        Controller-->>FE: HTTP 404 Not Found
        FE-->>Guest: แสดงแจ้งเตือน "ไม่พบรอบรับหิ้ว"
    else พบข้อมูลรอบรับหิ้ว
        SVC->>SVC: 8. แปลง Entity -> DeliveryRoundResponseDTO
        SVC-->>Controller: 9. Return DeliveryRoundResponseDTO
        deactivate SVC
        Controller-->>FE: 10. HTTP 200 OK (JSON Data)
        deactivate Controller
        FE-->>Guest: 11. แสดงรายละเอียดรอบรับหิ้ว (ร้านค้า, สถานที่, เวลา scheduledAt, ค่าหิ้ว serviceFee)
    end