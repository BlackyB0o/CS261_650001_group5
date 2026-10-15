# API Conventions (ข้อตกลงกลางของทุก Task)

เอกสารนี้กำหนดรูปแบบที่ API, DTO และ DAO ของทุก task ใช้ร่วมกัน เพื่อให้ชื่อ รูปแบบ response และสถานะตรงกัน
ตารางฐานข้อมูลอ้างอิงจาก [docs/database/ERD.md](../database/ERD.md)

---

## 1. ข้อกำหนดทั่วไป

| หัวข้อ | ข้อกำหนด |
|---|---|
| Base path | `/api/v1` |
| Content-Type | `application/json` (ยกเว้นอัปโหลดรูปใช้ `multipart/form-data`) |
| รหัส (id) | `Long` |
| จำนวนเงิน | `BigDecimal` ทศนิยม 2 ตำแหน่ง เช่น `20.00` |
| วันและเวลา | `LocalDateTime` รูปแบบ ISO-8601 เช่น `2026-10-25T14:00:00` |
| Enum | ส่งเป็น `String` ตัวพิมพ์ใหญ่ เช่น `"RUNNER"` |
| ภาษาของ `message` | อังกฤษ (ตรงกับ Bean Validation และเอกสาร Task04/08/16) หน้าเว็บเลือกแปลหรือแสดงตามที่ต้องการ |

---

## 2. Authentication และ Role

- ทุก endpoint ต้องแนบ `Authorization: Bearer <JWT_TOKEN>` ยกเว้น `register` และ `login` (Task04)
- JWT ต้องมีข้อมูล `userId` และ `role` (`RUNNER` หรือ `BUYER`)
- ค่าที่บอกว่า "ใครเป็นคนทำ" (`runnerId`, `buyerId`, `confirmedBy`) ดึงจาก JWT เสมอ **ห้ามรับจาก request body** เพื่อกันการปลอมเป็นผู้ใช้อื่น
- ไม่มี token หรือ token ไม่ถูกต้อง → `401` / role ไม่ตรงหรือไม่ใช่เจ้าของข้อมูล → `403`
- ผู้ดูแลระบบไม่ได้สมัครผ่าน `register` (รับเฉพาะ `RUNNER` และ `BUYER`)

---

## 3. รูปแบบ Response (`ApiResponseDTO<T>`)

### สำเร็จ
```json
{
  "status": "SUCCESS",
  "message": "Delivery round created successfully",
  "data": { }
}
```
- `message` ใส่เมื่อมีการสร้างหรือเปลี่ยนข้อมูล (POST/PATCH) ส่วน GET ไม่ต้องมี
- `errors` ไม่ส่งเมื่อสำเร็จ

### ผิดพลาด
```json
{
  "status": "ERROR",
  "message": "Validation failed",
  "errors": [
    { "field": "items[0].quantity", "message": "must be greater than or equal to 1" }
  ]
}
```
- `errors` ใช้เมื่อบอกได้ว่าผิดที่ field ไหน (ชื่อ field ตรงกับ JSON ที่ส่งมา) ถ้าเป็นข้อผิดพลาดอื่น เช่น `401`, `403`, `404`, `409` ส่งแค่ `status` กับ `message`
- `data` ไม่ส่งเมื่อผิดพลาด

```mermaid
classDiagram
    class ApiResponseDTO~T~ {
        -String status
        -String message
        -T data
        -List~FieldErrorDTO~ errors
        +getStatus() String
        +getMessage() String
        +getData() T
        +getErrors() List~FieldErrorDTO~
    }
    class FieldErrorDTO {
        -String field
        -String message
        +getField() String
        +getMessage() String
    }
    ApiResponseDTO ..> FieldErrorDTO : errors
```

### HTTP Status ที่ใช้

| Status | ใช้เมื่อ |
|---|---|
| `200 OK` | อ่านข้อมูลหรืออัปเดตสำเร็จ |
| `201 Created` | สร้างข้อมูลใหม่สำเร็จ |
| `400 Bad Request` | ข้อมูลไม่ครบหรือไม่ถูกรูปแบบ (ไม่บันทึกข้อมูล) |
| `401 Unauthorized` | ไม่มี token หรือ token ไม่ถูกต้อง/หมดอายุ |
| `403 Forbidden` | role ไม่ตรง หรือไม่ใช่เจ้าของข้อมูล |
| `404 Not Found` | ไม่พบข้อมูลตาม id |
| `409 Conflict` | ข้อมูลซ้ำ หรือสถานะปัจจุบันไม่อนุญาตให้ทำรายการนี้ |

---

## 4. Enum และสถานะที่ใช้ร่วมกัน

| Enum | ค่าที่ใช้ | ใช้ที่ |
|---|---|---|
| `Role` | `RUNNER` (ผู้รับหิ้ว), `BUYER` (ผู้ฝากหิ้ว) | `USERS.role` |
| `RoundStatus` | `OPEN`, `CLOSED`, `COMPLETED`, `CANCELLED` | `DELIVERY_ROUNDS.status` |
| `HandoffMethod` | `MEETUP` (นัดรับ), `DELIVERY` (ส่งถึงที่) | `ORDERS.handoff_method` |
| `OrderStatus` | `PENDING` (รอชำระเงิน), `PAID` (ชำระแล้ว), `COMPLETED` (ปิดออเดอร์) | `ORDERS.status` |
| `PaymentStatus` | `PENDING`, `HELD` (ชำระสำเร็จ เงินพักไว้ที่ระบบกลาง), `FAILED`, `RELEASED` (โอนให้ผู้รับหิ้วแล้ว) | `PAYMENTS.payment_status` |
| `PostStatus` | `OPEN`, `CLOSED` | `POSTS.status` (Task12) |
| `PaymentMethod` | `QR_TRANSFER` (สแกน QR ระบบตัวกลาง + แนบสลิป) | `PAYMENTS.payment_method` |
| `CompletionStatus` | `COMPLETED` | `ORDER_COMPLETIONS.completion_status` |

### ลำดับสถานะตลอดกระบวนการ

| ขั้นตอน | Task | Round | Order | Payment |
|---|---|---|---|---|
| 1. ผู้รับหิ้วเปิดรอบ | Task08 | `OPEN` | - | - |
| 2. ผู้ฝากหิ้วสร้างออเดอร์ในรอบ | Task16 | `OPEN` | `PENDING` | - |
| 3. ผู้รับหิ้วระบุราคาสินค้า | Task20 | เดิม | `PENDING` | `PENDING` |
| 4. ผู้ฝากหิ้วสแกน QR แนบสลิป ยอดและสลิปถูกต้อง | Task20 | `OPEN` หรือ `CLOSED` | `PAID` | `HELD` |
| 5. ผู้ฝากหิ้วยืนยันรับสินค้าพร้อมรูป | Task24 | เดิม | `COMPLETED` | `RELEASED` |

---

## 5. โครงสร้าง package ในโค้ด

ใช้ package หลักตามโปรเจกต์ปัจจุบันคือ `com.example.demo`

| package | เก็บอะไร |
|---|---|
| `com.example.demo.controller` | Controller (API) |
| `com.example.demo.service` | Service (business logic) |
| `com.example.demo.repository` | Repository / DAO (`JpaRepository`) |
| `com.example.demo.entity` | Entity ที่ map กับตารางใน ERD |
| `com.example.demo.dto` | DTO ทั้ง request และ response |
| `com.example.demo.service` (ส่วน `FileStorageService`) | เก็บไฟล์รูปที่อัปโหลด (สลิป Task20, หลักฐานรับสินค้า Task24) คืนค่าที่อยู่ไฟล์ |
| `com.example.demo.exception` | Exception และตัวจัดการ error ให้ออกเป็นรูปแบบในข้อ 3 |

Entity ที่ map กับตารางชื่อ `orders` ต้องระบุ `@Table(name = "orders")` เพราะ `order` เป็นคำสงวนของ SQL

---

## 6. Dependency ที่ต้องมีใน `pom.xml`

`pom.xml` ปัจจุบันมีแค่ web MVC, devtools, tomcat และ test ตามที่ design ไว้ต้องเพิ่ม (ให้คนดูแล `pom.xml` เป็นคนเพิ่มคนเดียว และตรวจชื่อ artifact ให้ตรงกับ Spring Boot 4.1.1)

- Spring Data JPA (`JpaRepository`)
- Bean Validation (`@NotBlank`, `@Min`, `@Future` ฯลฯ)
- Spring Security (ใช้ `PasswordEncoder` สำหรับ BCrypt และตรวจ JWT)
- PostgreSQL JDBC driver
- ไลบรารี JWT (เช่น `jjwt`)
