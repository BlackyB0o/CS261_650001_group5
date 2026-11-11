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
```


## Test Cases (US02)

| Test Case ID | หัวข้อการทดสอบ (Test Scenario) | เงื่อนไข / Input Data | ผลลัพธ์ที่คาดหวัง (Expected Output) | HTTP Status |
| :--- | :--- | :--- | :--- | :---: |
| **TC-US02-01** | ค้นหารอบรับหิ้วด้วย `keyword` | `GET /api/v1/delivery-rounds?keyword=ชานม` | คืนค่ารายการรอบรับหิ้วที่มีชื่อร้าน, สถานที่ หรือชื่อผู้รับหิ้วตรงกับคำว่า "ชานม" | `200 OK` |
| **TC-US02-02** | ค้นหาโดยไม่ส่งค่า `status` | `GET /api/v1/delivery-rounds?keyword=SC1` | ระบบจะกรองและคืนค่าเฉพาะรอบรับหิ้วที่มีสถานะเป็น `OPEN` เท่านั้น | `200 OK` |
| **TC-US02-03** | ค้นหารอบรับหิ้วโดยระบุ `date` และ `keyword` | `GET /api/v1/delivery-rounds?keyword=SC1&date=2026-11-12` | คืนค่ารอบที่มีสถานะ `OPEN` ในวันที่ 2026-11-12 ที่ตรงกับคำค้นหา | `200 OK` |
| **TC-US02-04** | ทดสอบการแบ่งหน้า (Pagination) | `GET /api/v1/delivery-rounds?page=2&size=10` | คืนค่าข้อมูลรอบรับหิ้วหน้า 2 จำนวน 10 รายการ พร้อมวัตถุ `pagination` (`page: 2, size: 10`) | `200 OK` |
| **TC-US02-05** | ค้นหาด้วย `keyword` ที่มีอักขระพิเศษ `%` หรือ `_` | `GET /api/v1/delivery-rounds?keyword=100%` | มอง `%` เป็นตัวอักษรปกติ ไม่ใช้เป็น Wildcard และคืนค่าเฉพาะข้อมูลที่มีอักขระ `%` | `200 OK` |
| **TC-US02-06** | ค้นหาแล้วไม่พบข้อมูลรอบรับหิ้ว | `GET /api/v1/delivery-rounds?keyword=ร้านที่ไม่มีอยู่จริง` | คืนค่า `data: []` และ `totalItems: 0` ในส่วน pagination | `200 OK` |
| **TC-US02-07** | Validation Error: รูปแบบวันที่ไม่ถูกต้อง (`date`) | `GET /api/v1/delivery-rounds?date=2026-02-30` | คืนค่า Error Message `"date: must be a valid date in yyyy-MM-dd format"` | `400 Bad Request` |
| **TC-US02-08** | Validation Error: ค่า `page` หรือ `size` ไม่อยู่ในเกณฑ์ | `GET /api/v1/delivery-rounds?page=0&size=150` | คืนค่า Validation Error สำหรับ `page` ต้อง >= 1 และ `size` อยู่ระหว่าง 1 ถึง 100 | `400 Bad Request` |
| **TC-US02-09** | ดูรายละเอียดรอบรับหิ้วด้วย ID ที่มีอยู่จริง | `GET /api/v1/delivery-rounds/101` | คืนค่าข้อมูลรอบรับหิ้ว ID 101 ครบถ้วน | `200 OK` |
| **TC-US02-10** | ดูรายละเอียดรอบรับหิ้วด้วย ID ที่ไม่มีในระบบ | `GET /api/v1/delivery-rounds/9999` | คืนค่า Error Message `"Delivery round not found"` | `404 Not Found` |