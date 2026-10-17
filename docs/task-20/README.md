# Task20: US07 - ชำระค่าสินค้าและค่าหิ้วผ่านระบบตัวกลาง (API, API Class Diagram, DTO, DAO)

> ใช้ร่วมกับ [API Conventions](../common/API-CONVENTIONS.md) และ [ERD](../database/ERD.md) (ตาราง `PAYMENTS`, `ORDERS`)
> ต่อจาก Task16 (ออเดอร์สถานะ `PENDING`) และส่งต่อให้ Task24 (ปล่อยเงินเมื่อยืนยันรับสินค้า)

## แนวคิดการชำระเงิน (เพื่อความปลอดภัยและกันการฉ้อโกง)

1. **ผู้รับหิ้วเป็นคนระบุราคาสินค้า** ของออเดอร์ (หลังเห็นรายการที่ผู้ฝากหิ้วสั่ง) ผู้ฝากหิ้วแก้ราคาเองไม่ได้
2. ระบบสร้างรายการชำระเงิน (`Payment` สถานะ `PENDING`) และออก **QR Code ของระบบตัวกลาง** ที่มียอดรวม = ราคาสินค้า + ค่าหิ้ว
3. ผู้ฝากหิ้ว **สแกน QR จ่ายเงินเข้าระบบตัวกลาง (ไม่โอนให้ผู้รับหิ้วโดยตรง)** แล้ว **แนบสลิป** เพื่อยืนยัน
4. เมื่อสลิปและยอดถูกต้อง เงินสถานะ `HELD` (พักไว้ที่ระบบกลาง) และออเดอร์เป็น `PAID` เงินจะถูกโอนให้ผู้รับหิ้วเมื่อผู้ฝากหิ้วยืนยันรับสินค้า (Task24)

ใน Sprint 1 การตรวจสลิปเป็นแบบง่าย: ต้องแนบไฟล์รูปที่ถูกต้อง และยอดที่ผู้ฝากหิ้วระบุต้องตรงกับยอดรวม (ไม่อ่านข้อมูลในรูปด้วย OCR และไม่เชื่อมธนาคารจริง)

## 0. เงื่อนไขจาก User Story ที่ API ต้องรองรับ

| Test case ใน US07 | สิ่งที่ API ต้องทำ | Response |
|---|---|---|
| 1. ผู้ฝากหิ้วชำระค่าสินค้าและค่าหิ้วผ่านระบบตัวกลางได้สำเร็จ | รับสลิป เปลี่ยน `Payment` เป็น `HELD` และออเดอร์เป็น `PAID` | `201 Created` |
| 2. ระบบไม่ยืนยันการชำระเงิน หากชำระไม่สำเร็จหรือยอดเงินไม่ถูกต้อง | ไม่เปลี่ยนสถานะใด ๆ (ออเดอร์ยัง `PENDING`) | `400 Bad Request` |
| So that 1: เงินไม่สูญหายหากไม่ได้รับสินค้า | เงินสถานะ `HELD` อยู่กับระบบกลาง | - |
| So that 2: โอนให้ผู้รับหิ้วเมื่อคำสั่งซื้อเสร็จสมบูรณ์เท่านั้น | ยังไม่โอน ต้องรอ Task24 | - |

---

## 1. API Specification

### Endpoint 1: Set Product Price (ผู้รับหิ้วระบุราคาสินค้าของออเดอร์)
- **URL:** `/api/v1/orders/{orderId}/price`
- **Method:** `PUT`
- **Headers:** `Authorization: Bearer <JWT_TOKEN>` (เจ้าของรอบที่ออเดอร์อยู่เท่านั้น)
- **Content-Type:** `application/json`
- **Request Body:**
```json
{
  "productAmount": 80.00
}
```
**กติกาตรวจข้อมูล**

| Field | กติกา | ตัวอย่าง message |
|---|---|---|
| `productAmount` | ห้ามว่าง และต้อง > 0.00 | `must be greater than 0.00` |

**สิ่งที่ระบบทำ:** ตรวจว่าเป็นเจ้าของรอบและออเดอร์ยัง `PENDING` แล้วสร้าง `Payment` (ถ้ายังไม่มี) หรือแก้ราคาในแถวเดิม โดยตั้ง `service_fee` จากรอบหิ้ว, `total_amount` = `productAmount` + `serviceFee`, `payment_method` = `QR_TRANSFER`, `payment_status` = `PENDING` และสร้างข้อมูล QR สำหรับยอดนี้ แก้ราคาได้จนกว่าผู้ฝากหิ้วจะชำระสำเร็จ

- **Response (200 OK):**
```json
{
  "status": "SUCCESS",
  "message": "Product price set successfully",
  "data": {
    "paymentId": 3001,
    "orderId": 5001,
    "productAmount": 80.00,
    "serviceFee": 20.00,
    "totalAmount": 100.00,
    "paymentMethod": "QR_TRANSFER",
    "paymentStatus": "PENDING",
    "qrPayload": "CARRYHUB-PAY|3001|100.00",
    "slipImageUrl": null,
    "transactionReference": null,
    "paidAt": null,
    "orderStatus": "PENDING"
  }
}
```

| HTTP Status | เงื่อนไข | ตัวอย่าง message |
|---|---|---|
| `400 Bad Request` | ข้อมูลไม่ครบหรือไม่ถูกรูปแบบ | `Validation failed` พร้อม `errors` |
| `401 Unauthorized` | ไม่มี token | `Invalid or missing token` |
| `403 Forbidden` | ไม่ใช่เจ้าของรอบของออเดอร์นี้ | `You are not the owner of this round` |
| `404 Not Found` | ไม่พบออเดอร์ | `Order not found` |
| `409 Conflict` | ออเดอร์ไม่ใช่ `PENDING` (ชำระแล้วหรือปิดแล้ว) | `Order has already been paid` |

### Endpoint 2: Get Order Payment (ดูยอดชำระและ QR)
- **URL:** `/api/v1/orders/{orderId}/payment`
- **Method:** `GET`
- **Headers:** `Authorization: Bearer <JWT_TOKEN>` (ผู้ฝากหิ้วเจ้าของออเดอร์ หรือผู้รับหิ้วเจ้าของรอบ)
- **Response (200 OK):** `data` รูปแบบเดียวกับ Endpoint 1 ผู้ฝากหิ้วใช้ `qrPayload` สร้าง QR ให้สแกน
- **Error:**

| HTTP Status | เงื่อนไข | ตัวอย่าง message |
|---|---|---|
| `401 Unauthorized` | ไม่มี token | `Invalid or missing token` |
| `403 Forbidden` | ไม่ใช่ผู้เกี่ยวข้องกับออเดอร์นี้ | `You are not allowed to view this payment` |
| `404 Not Found` | ไม่พบออเดอร์ หรือผู้รับหิ้วยังไม่ระบุราคา | `Payment not found` |

### Endpoint 3: Submit Payment Slip (ผู้ฝากหิ้วแนบสลิปเพื่อยืนยันการชำระเงิน)
- **URL:** `/api/v1/orders/{orderId}/payment/slip`
- **Method:** `POST`
- **Headers:** `Authorization: Bearer <JWT_TOKEN>` (ผู้ฝากหิ้วเจ้าของออเดอร์เท่านั้น)
- **Content-Type:** `multipart/form-data`
- **Request Parts:**

| Part | ชนิด | บังคับ | กติกา |
|---|---|---|---|
| `slipImage` | ไฟล์รูป | ใช่ | ต้องแนบ, ชนิด JPEG หรือ PNG, ไม่เกิน 5 MB |
| `paidAmount` | ตัวเลข (BigDecimal) | ใช่ | > 0.00 และต้องเท่ากับ `totalAmount` ของรายการชำระ |

**สิ่งที่ระบบทำเมื่อสำเร็จ (ใน transaction เดียว)**
1. ตรวจเจ้าของออเดอร์, ออเดอร์ `PENDING`, `Payment` สถานะ `PENDING` (ผู้รับหิ้วระบุราคาแล้ว)
2. ตรวจไฟล์สลิปและเทียบ `paidAmount` กับ `totalAmount`
3. เก็บไฟล์ บันทึก `slip_image_url`, `transaction_reference`, `paid_at`
4. เปลี่ยน `Payment` เป็น `HELD` และ `Order` เป็น `PAID`

- **Response (201 Created):**
```json
{
  "status": "SUCCESS",
  "message": "Payment confirmed successfully",
  "data": {
    "paymentId": 3001,
    "orderId": 5001,
    "productAmount": 80.00,
    "serviceFee": 20.00,
    "totalAmount": 100.00,
    "paymentMethod": "QR_TRANSFER",
    "paymentStatus": "HELD",
    "qrPayload": "CARRYHUB-PAY|3001|100.00",
    "slipImageUrl": "/uploads/slips/5001-1761373800.jpg",
    "transactionReference": "TXN-20261025-000123",
    "paidAt": "2026-10-25T13:30:00",
    "orderStatus": "PAID"
  }
}
```

| HTTP Status | เงื่อนไข | ตัวอย่าง message |
|---|---|---|
| `400 Bad Request` | ไม่แนบสลิป / ไม่ใช่ JPEG หรือ PNG / เกิน 5 MB / `paidAmount` ไม่เท่ายอดรวม (ไม่เปลี่ยนสถานะ ตาม Test case 2) | `Validation failed` พร้อม `errors` |
| `401 Unauthorized` | ไม่มี token | `Invalid or missing token` |
| `403 Forbidden` | ไม่ใช่เจ้าของออเดอร์ | `You are not the owner of this order` |
| `404 Not Found` | ไม่พบออเดอร์ | `Order not found` |
| `409 Conflict` | ผู้รับหิ้วยังไม่ระบุราคา | `Product price has not been set yet` |
| `409 Conflict` | ออเดอร์ชำระแล้วหรือปิดแล้ว | `Order has already been paid` |

ตัวอย่าง `400`:
```json
{
  "status": "ERROR",
  "message": "Validation failed",
  "errors": [
    { "field": "slipImage", "message": "must not be empty" },
    { "field": "paidAmount", "message": "must equal the total amount (100.00)" }
  ]
}
```
ข้อความอื่นของ `slipImage`: `only JPEG or PNG images are allowed`, `file size must not exceed 5 MB`

---

## 2. API Class Diagram
```mermaid
classDiagram
    class PaymentController {
        +setProductPrice(Long orderId, SetProductPriceRequestDTO request) ResponseEntity~ApiResponseDTO~
        +getPayment(Long orderId) ResponseEntity~ApiResponseDTO~
        +submitSlip(Long orderId, SubmitSlipRequestDTO request) ResponseEntity~ApiResponseDTO~
    }
    class SetProductPriceRequestDTO {
        +BigDecimal productAmount
    }
    class SubmitSlipRequestDTO {
        +MultipartFile slipImage
        +BigDecimal paidAmount
    }
    class PaymentResponseDTO {
        +Long paymentId
        +Long orderId
        +BigDecimal productAmount
        +BigDecimal serviceFee
        +BigDecimal totalAmount
        +String paymentMethod
        +String paymentStatus
        +String qrPayload
        +String slipImageUrl
        +String transactionReference
        +LocalDateTime paidAt
        +String orderStatus
    }
    class ApiResponseDTO~T~ {
        +String status
        +String message
        +T data
    }

    PaymentController ..> SetProductPriceRequestDTO : receives
    PaymentController ..> SubmitSlipRequestDTO : receives
    PaymentController ..> ApiResponseDTO : returns
    ApiResponseDTO ..> PaymentResponseDTO : wraps
```

---

## 3. DTO Class Specification & Diagram

1. **`SetProductPriceRequestDTO`** (JSON)
   - `productAmount`: BigDecimal (Not Null, > 0.00)
2. **`SubmitSlipRequestDTO`** (`multipart/form-data` ผูกด้วย `@ModelAttribute`)
   - `slipImage`: MultipartFile (Not Null, ไม่ว่าง, `image/jpeg` หรือ `image/png`, ไม่เกิน 5 MB)
   - `paidAmount`: BigDecimal (Not Null, > 0.00)
3. **`PaymentResponseDTO`**
   - `paymentId`, `orderId`: Long
   - `productAmount`, `serviceFee`, `totalAmount`: BigDecimal
   - `paymentMethod`: String (`QR_TRANSFER`)
   - `paymentStatus`: String (`PENDING` หรือ `HELD`)
   - `qrPayload`: String (ข้อมูลสำหรับสร้าง QR ของระบบตัวกลาง)
   - `slipImageUrl`: String (ว่างก่อนแนบสลิป)
   - `transactionReference`: String (ว่างก่อนชำระ)
   - `paidAt`: LocalDateTime (ว่างก่อนชำระ)
   - `orderStatus`: String

ขนาดไฟล์ 5 MB ตั้งที่ `spring.servlet.multipart.max-file-size` และ `max-request-size`

```mermaid
classDiagram
    class SetProductPriceRequestDTO {
        -BigDecimal productAmount
        +getProductAmount() BigDecimal
    }
    class SubmitSlipRequestDTO {
        -MultipartFile slipImage
        -BigDecimal paidAmount
        +getSlipImage() MultipartFile
        +getPaidAmount() BigDecimal
    }
    class PaymentResponseDTO {
        -Long paymentId
        -Long orderId
        -BigDecimal productAmount
        -BigDecimal serviceFee
        -BigDecimal totalAmount
        -String paymentMethod
        -String paymentStatus
        -String qrPayload
        -String slipImageUrl
        -String transactionReference
        -LocalDateTime paidAt
        -String orderStatus
        +getPaymentId() Long
        +getOrderId() Long
        +getProductAmount() BigDecimal
        +getServiceFee() BigDecimal
        +getTotalAmount() BigDecimal
        +getPaymentMethod() String
        +getPaymentStatus() String
        +getQrPayload() String
        +getSlipImageUrl() String
        +getTransactionReference() String
        +getPaidAt() LocalDateTime
        +getOrderStatus() String
    }
```

---

## 4. DAO / Repository Specification & Diagram

### Entity `Payment` (ตาราง `payments`)

| Field | Column | หมายเหตุ |
|---|---|---|
| `id` | `id` | PK |
| `order` (`Order`) | `order_id` | FK → `ORDERS.id`, UNIQUE (`@OneToOne`) |
| `productAmount` | `product_amount` | NOT NULL ราคาที่ผู้รับหิ้วระบุ |
| `serviceFee` | `service_fee` | NOT NULL คัดลอกจากรอบหิ้ว |
| `totalAmount` | `total_amount` | NOT NULL |
| `paymentMethod` | `payment_method` | NOT NULL (`QR_TRANSFER`) |
| `paymentStatus` | `payment_status` | NOT NULL (`PENDING`, `HELD`, `FAILED`, `RELEASED`) |
| `slipImageUrl` | `slip_image_url` | **column ใหม่** ว่างได้จนกว่าจะแนบสลิป |
| `transactionReference` | `transaction_reference` | ว่างได้ก่อนชำระ |
| `paidAt` | `paid_at` | ว่างได้ก่อนชำระ |

> `slip_image_url` ยังไม่มีใน ERD ต้องเพิ่มที่ตาราง `PAYMENTS` เพื่อเก็บสลิปที่ผู้ฝากหิ้วแนบ (ใช้ตรวจสอบย้อนหลังกรณีมีข้อพิพาท) ส่วน `FAILED` เก็บไว้ใน enum ตาม ERD เดิม แต่ Sprint 1 ยังไม่ใช้ เพราะสลิปที่ไม่ผ่านจะตอบ `400` และไม่เปลี่ยนแถว

### Repository
- **Interface:** `PaymentRepository` (extends `JpaRepository<Payment, Long>`)
- **Methods:**
  - `Payment save(Payment payment)` - สร้าง/แก้ราคา (Endpoint 1), บันทึกสลิป (Endpoint 3), เปลี่ยนเป็น `RELEASED` (Task24)
  - `Optional<Payment> findByOrderId(Long orderId)` - ดึงรายการชำระของออเดอร์ (ทุก endpoint และ Task24)
- **ใช้ร่วมกับ task อื่น:** `OrderRepository.findById/save` (Task16) ตรวจเจ้าของและเปลี่ยนเป็น `PAID`, `DeliveryRoundRepository` (Task08) ดึง `serviceFee` และตรวจเจ้าของรอบผ่าน `order.round`

```mermaid
classDiagram
    class PaymentRepository {
        <<interface>>
        +save(Payment payment) Payment
        +findByOrderId(Long orderId) Optional~Payment~
    }
    class Payment {
        -Long id
        -Order order
        -BigDecimal productAmount
        -BigDecimal serviceFee
        -BigDecimal totalAmount
        -String paymentMethod
        -String paymentStatus
        -String slipImageUrl
        -String transactionReference
        -LocalDateTime paidAt
    }
    class Order {
        -Long id
        -String status
    }
    PaymentRepository ..> Payment : manages
    Payment "1" --> "1" Order : order
```

---

## 5. Extended API Class Diagram (Full Layered Architecture)

Controller เรียก Service, Service ใช้ `PaymentRepository`, `OrderRepository` (Task16) และ `FileStorageService` (เก็บไฟล์รูป ใช้ร่วมกับ Task24)

```mermaid
classDiagram
    class PaymentController {
        -PaymentService paymentService
        +setProductPrice(Long orderId, SetProductPriceRequestDTO request) ResponseEntity~ApiResponseDTO~
        +getPayment(Long orderId) ResponseEntity~ApiResponseDTO~
        +submitSlip(Long orderId, SubmitSlipRequestDTO request) ResponseEntity~ApiResponseDTO~
    }
    class PaymentService {
        <<interface>>
        +setProductPrice(Long runnerId, Long orderId, SetProductPriceRequestDTO request) PaymentResponseDTO
        +getPayment(Long userId, Long orderId) PaymentResponseDTO
        +submitSlip(Long buyerId, Long orderId, SubmitSlipRequestDTO request) PaymentResponseDTO
    }
    class PaymentServiceImpl {
        -PaymentRepository paymentRepository
        -OrderRepository orderRepository
        -FileStorageService fileStorageService
    }
    class FileStorageService {
        <<interface>>
        +store(MultipartFile file, String folder) String
    }
    class PaymentRepository {
        <<interface>>
        +save(Payment payment) Payment
        +findByOrderId(Long orderId) Optional~Payment~
    }
    class OrderRepository {
        <<interface>>
        +findById(Long id) Optional~Order~
        +save(Order order) Order
    }
    class Payment {
        -Long id
        -Order order
        -BigDecimal productAmount
        -BigDecimal serviceFee
        -BigDecimal totalAmount
        -String paymentMethod
        -String paymentStatus
        -String slipImageUrl
        -String transactionReference
        -LocalDateTime paidAt
    }
    class Order {
        -Long id
        -DeliveryRound round
        -String status
    }
    class SetProductPriceRequestDTO
    class SubmitSlipRequestDTO
    class PaymentResponseDTO
    class ApiResponseDTO~T~ {
        -String status
        -String message
        -T data
        -List~FieldErrorDTO~ errors
    }

    PaymentController --> PaymentService : uses
    PaymentController ..> ApiResponseDTO : returns
    PaymentServiceImpl ..|> PaymentService : implements
    PaymentServiceImpl --> PaymentRepository : uses
    PaymentServiceImpl --> OrderRepository : uses
    PaymentServiceImpl --> FileStorageService : uses
    PaymentRepository ..> Payment : manages
    OrderRepository ..> Order : manages
    Payment "1" --> "1" Order : order
    PaymentServiceImpl ..> SetProductPriceRequestDTO : accepts
    PaymentServiceImpl ..> SubmitSlipRequestDTO : accepts
    PaymentServiceImpl ..> PaymentResponseDTO : creates
    ApiResponseDTO ..> PaymentResponseDTO : wraps
```
