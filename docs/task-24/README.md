# Task24: US08 - ยืนยันการได้รับสินค้าและปิดออเดอร์ (API, API Class Diagram, DTO, DAO)

> ใช้ร่วมกับ [API Conventions](../common/API-CONVENTIONS.md) (รูปแบบ response, JWT, enum) และ [ERD](../database/ERD.md) (ตาราง `ORDER_COMPLETIONS`, `PAYMENTS`, `ORDERS`)
> ต่อจาก Task16 (ออเดอร์) และ Task20 (ชำระเงิน) ออเดอร์ต้องอยู่ในสถานะ `PAID` และมีการชำระเงินสถานะ `HELD` จึงจะยืนยันรับสินค้าได้

## 0. เงื่อนไขจาก User Story ที่ API ต้องรองรับ

| Test case ใน US08 | สิ่งที่ API ต้องทำ | Response |
|---|---|---|
| 1. ผู้ฝากหิ้วถ่ายรูปและส่งรูปเพื่อยืนยันว่าได้รับสินค้าเรียบร้อยแล้ว | บันทึกการยืนยันพร้อมรูป ปิดออเดอร์ และปล่อยเงินให้ผู้รับหิ้ว | `201 Created` |
| 2. ไม่มีรูปภาพหรือหลักฐานที่แน่ชัด ระบบไม่อนุญาตให้ปิดออเดอร์ | ไม่บันทึกและไม่เปลี่ยนสถานะใด ๆ | `400 Bad Request` |
| So that: โอนเงินค่าสินค้าพร้อมค่าหิ้วให้ผู้รับหิ้ว และปิดออเดอร์สมบูรณ์ไม่มีข้อพิพาทค้าง | `Payment` เปลี่ยนเป็น `RELEASED`, `Order` เปลี่ยนเป็น `COMPLETED` | อยู่ใน Endpoint 1 |

---

## 1. API Specification

### Endpoint 1: Confirm Receipt and Close Order (ยืนยันรับสินค้าและปิดออเดอร์)
- **URL:** `/api/v1/orders/{orderId}/completion`
- **Method:** `POST`
- **Headers:** `Authorization: Bearer <JWT_TOKEN>` (ผู้ฝากหิ้วเจ้าของออเดอร์เท่านั้น)
- **Content-Type:** `multipart/form-data`
- **Request Parts:**

| Part | ชนิด | บังคับ | กติกา |
|---|---|---|---|
| `evidenceImage` | ไฟล์รูป | ใช่ | ต้องแนบไฟล์, ชนิด JPEG หรือ PNG, ขนาดไม่เกิน 5 MB |

*(หมายเหตุ: `confirmedBy` ไม่ต้องส่ง ระบบอ่านจาก JWT)*

**สิ่งที่ระบบทำเมื่อสำเร็จ (ใน transaction เดียว ถ้าขั้นใดล้มเหลวให้ rollback ทั้งหมด)**
1. ตรวจว่าผู้ใช้เป็นเจ้าของออเดอร์ และออเดอร์อยู่ในสถานะ `PAID`
2. ตรวจไฟล์รูป (ต้องมี ชนิด และขนาดถูกต้อง) แล้วเก็บไฟล์
3. บันทึก `OrderCompletion` (`completion_status = COMPLETED`, `confirmed_by` = ผู้ใช้จาก JWT, `confirmed_at` = เวลาปัจจุบัน, `evidence_image_url` = ที่อยู่ไฟล์)
4. เปลี่ยน `Payment.paymentStatus` จาก `HELD` เป็น `RELEASED` (เงินค่าสินค้าพร้อมค่าหิ้วไปที่ผู้รับหิ้ว)
5. เปลี่ยน `Order.status` จาก `PAID` เป็น `COMPLETED`

- **Response (201 Created):**
```json
{
  "status": "SUCCESS",
  "message": "Order completed successfully",
  "data": {
    "completionId": 701,
    "orderId": 5001,
    "paymentId": 3001,
    "confirmedBy": 2,
    "confirmedAt": "2026-10-25T15:10:00",
    "completionStatus": "COMPLETED",
    "evidenceImageUrl": "/uploads/completions/5001-1761376200.jpg",
    "runnerId": 1,
    "releasedAmount": 100.00,
    "orderStatus": "COMPLETED",
    "paymentStatus": "RELEASED"
  }
}
```

**Error Responses**

| HTTP Status | เงื่อนไข | ตัวอย่าง message |
|---|---|---|
| `400 Bad Request` | ไม่แนบรูป / ไม่ใช่ JPEG หรือ PNG / ขนาดเกิน 5 MB (ไม่บันทึกและไม่ปิดออเดอร์) | `Validation failed` พร้อม `errors` ระบุ field `evidenceImage` |
| `401 Unauthorized` | ไม่ส่ง token หรือ token ไม่ถูกต้อง | `Invalid or missing token` |
| `403 Forbidden` | ไม่ใช่เจ้าของออเดอร์นี้ | `You are not the owner of this order` |
| `404 Not Found` | ไม่พบออเดอร์ตาม `orderId` | `Order not found` |
| `409 Conflict` | ออเดอร์ยังไม่ได้ชำระเงิน (สถานะ `PENDING`) | `Order has not been paid` |
| `409 Conflict` | ออเดอร์ถูกปิดไปแล้ว (สถานะ `COMPLETED`) | `Order is already completed` |

ตัวอย่าง `400`:
```json
{
  "status": "ERROR",
  "message": "Validation failed",
  "errors": [
    { "field": "evidenceImage", "message": "must not be empty" }
  ]
}
```
ข้อความอื่นของ field `evidenceImage`: `only JPEG or PNG images are allowed`, `file size must not exceed 5 MB`

### Endpoint 2: Get Order Completion (ดูผลการยืนยันรับสินค้า)
- **URL:** `/api/v1/orders/{orderId}/completion`
- **Method:** `GET`
- **Headers:** `Authorization: Bearer <JWT_TOKEN>` (ผู้ฝากหิ้วเจ้าของออเดอร์ หรือผู้รับหิ้วเจ้าของรอบที่ออเดอร์อยู่)
- **Response (200 OK):** `data` รูปแบบเดียวกับ `data` ใน Endpoint 1
- **Error:**

| HTTP Status | เงื่อนไข | ตัวอย่าง message |
|---|---|---|
| `401 Unauthorized` | ไม่มี token | `Invalid or missing token` |
| `403 Forbidden` | ไม่ใช่ผู้เกี่ยวข้องกับออเดอร์นี้ | `You are not allowed to view this order` |
| `404 Not Found` | ไม่พบออเดอร์ หรือออเดอร์ยังไม่ได้ยืนยันรับสินค้า | `Order completion not found` |

---

## 2. API Class Diagram
```mermaid
classDiagram
    class OrderCompletionController {
        +confirmReceipt(Long orderId, ConfirmReceiptRequestDTO request) ResponseEntity~ApiResponseDTO~
        +getCompletion(Long orderId) ResponseEntity~ApiResponseDTO~
    }
    class ConfirmReceiptRequestDTO {
        +MultipartFile evidenceImage
    }
    class CompletionResponseDTO {
        +Long completionId
        +Long orderId
        +Long paymentId
        +Long confirmedBy
        +LocalDateTime confirmedAt
        +String completionStatus
        +String evidenceImageUrl
        +Long runnerId
        +BigDecimal releasedAmount
        +String orderStatus
        +String paymentStatus
    }
    class ApiResponseDTO~T~ {
        +String status
        +String message
        +T data
    }

    OrderCompletionController ..> ConfirmReceiptRequestDTO : receives
    OrderCompletionController ..> ApiResponseDTO : returns
    ApiResponseDTO ..> CompletionResponseDTO : wraps
```

---

## 3. DTO Class Specification & Diagram

`ApiResponseDTO<T>` และ `FieldErrorDTO` ใช้ร่วมกันทุก task ดูที่ [API Conventions](../common/API-CONVENTIONS.md)

### DTO Specifications
1. **`ConfirmReceiptRequestDTO`** (รับแบบ `multipart/form-data` ผูกด้วย `@ModelAttribute`)
   - `evidenceImage`: MultipartFile (Not Null, ไม่เป็นไฟล์ว่าง, ชนิด `image/jpeg` หรือ `image/png`, ขนาดไม่เกิน 5 MB)
2. **`CompletionResponseDTO`**
   - `completionId`: Long
   - `orderId`: Long
   - `paymentId`: Long
   - `confirmedBy`: Long (ผู้ฝากหิ้วที่กดยืนยัน)
   - `confirmedAt`: LocalDateTime
   - `completionStatus`: String (`COMPLETED`)
   - `evidenceImageUrl`: String
   - `runnerId`: Long (ผู้รับหิ้วที่ได้รับเงิน ดึงจากรอบของออเดอร์)
   - `releasedAmount`: BigDecimal (เท่ากับ `PAYMENTS.total_amount`)
   - `orderStatus`: String (`COMPLETED`)
   - `paymentStatus`: String (`RELEASED`)

ขนาดไฟล์ 5 MB ต้องตั้งค่าที่ `spring.servlet.multipart.max-file-size` และ `spring.servlet.multipart.max-request-size` ใน `application.properties` ด้วย

### DTO Class Diagram
```mermaid
classDiagram
    class ConfirmReceiptRequestDTO {
        -MultipartFile evidenceImage
        +getEvidenceImage() MultipartFile
    }

    class CompletionResponseDTO {
        -Long completionId
        -Long orderId
        -Long paymentId
        -Long confirmedBy
        -LocalDateTime confirmedAt
        -String completionStatus
        -String evidenceImageUrl
        -Long runnerId
        -BigDecimal releasedAmount
        -String orderStatus
        -String paymentStatus
        +getCompletionId() Long
        +getOrderId() Long
        +getPaymentId() Long
        +getConfirmedBy() Long
        +getConfirmedAt() LocalDateTime
        +getCompletionStatus() String
        +getEvidenceImageUrl() String
        +getRunnerId() Long
        +getReleasedAmount() BigDecimal
        +getOrderStatus() String
        +getPaymentStatus() String
    }
```

---

## 4. DAO / Repository Specification & Diagram

### Entity ที่เกี่ยวข้อง

**`OrderCompletion`** (ตาราง `order_completions`)

| Field | Column | หมายเหตุ |
|---|---|---|
| `id` | `id` | PK |
| `payment` (`Payment`) | `payment_id` | FK → `PAYMENTS.id`, UNIQUE (`@OneToOne`) หนึ่งการชำระเงินปิดได้ครั้งเดียว |
| `confirmedBy` (`User`) | `confirmed_by` | FK → `USERS.id` (`@ManyToOne`) |
| `confirmedAt` | `confirmed_at` | NOT NULL, DEFAULT CURRENT_TIMESTAMP |
| `completionStatus` | `completion_status` | NOT NULL (`COMPLETED`) |
| `evidenceImageUrl` | `evidence_image_url` | **column ใหม่** NOT NULL เก็บที่อยู่รูปหลักฐานตาม Test case 1 และ 2 |

> `evidence_image_url` ยังไม่มีใน ERD ต้องเพิ่มที่ตาราง `ORDER_COMPLETIONS` ใน [docs/database/ERD.md](../database/ERD.md) เพราะ US08 บังคับแนบรูปหลักฐาน

### Repository
- **Interface:** `OrderCompletionRepository` (extends `JpaRepository<OrderCompletion, Long>`)
- **Methods:**
  - `OrderCompletion save(OrderCompletion completion)` - บันทึกการยืนยันรับสินค้า (Endpoint 1)
  - `Optional<OrderCompletion> findByPaymentOrderId(Long orderId)` - ดูผลการยืนยันของออเดอร์ (Endpoint 2) ค้นผ่าน `payment.order.id`
  - `boolean existsByPaymentId(Long paymentId)` - กันการยืนยันซ้ำ
- **ใช้ร่วมกับ task อื่น (อยู่ใน transaction เดียวกัน):**
  - `OrderRepository.findById` / `save` (Task16) ตรวจเจ้าของ และเปลี่ยนสถานะออเดอร์เป็น `COMPLETED`
  - `PaymentRepository.findByOrderId` / `save` (Task20) ดึงการชำระเงินของออเดอร์ และเปลี่ยนสถานะเป็น `RELEASED`
  - `UserRepository.findById` (Task04) ดึงผู้ฝากหิ้วจาก JWT

```mermaid
classDiagram
    class OrderCompletionRepository {
        <<interface>>
        +save(OrderCompletion completion) OrderCompletion
        +findByPaymentOrderId(Long orderId) Optional~OrderCompletion~
        +existsByPaymentId(Long paymentId) boolean
    }
    class OrderCompletion {
        -Long id
        -Payment payment
        -User confirmedBy
        -LocalDateTime confirmedAt
        -String completionStatus
        -String evidenceImageUrl
    }
    class Payment {
        -Long id
        -Order order
        -BigDecimal totalAmount
        -String paymentStatus
    }
    class Order {
        -Long id
        -String status
    }
    class User {
        -Long id
        -String fullName
    }

    OrderCompletionRepository ..> OrderCompletion : manages
    OrderCompletion "1" --> "1" Payment : payment
    OrderCompletion --> User : confirmedBy
    Payment --> Order : order
```

---

## 5. Extended API Class Diagram (Full Layered Architecture)

Controller เรียก Service, Service ทำงานข้าม 3 ตารางใน transaction เดียว (`OrderCompletion`, `Payment`, `Order`) และใช้ `FileStorageService` ร่วมกับ Task20

```mermaid
classDiagram
    class OrderCompletionController {
        -OrderCompletionService completionService
        +confirmReceipt(Long orderId, ConfirmReceiptRequestDTO request) ResponseEntity~ApiResponseDTO~
        +getCompletion(Long orderId) ResponseEntity~ApiResponseDTO~
    }
    class OrderCompletionService {
        <<interface>>
        +confirmReceipt(Long buyerId, Long orderId, ConfirmReceiptRequestDTO request) CompletionResponseDTO
        +getCompletion(Long userId, Long orderId) CompletionResponseDTO
    }
    class OrderCompletionServiceImpl {
        -OrderCompletionRepository completionRepository
        -PaymentRepository paymentRepository
        -OrderRepository orderRepository
        -UserRepository userRepository
        -FileStorageService fileStorageService
    }
    class FileStorageService {
        <<interface>>
        +store(MultipartFile file, String folder) String
    }
    class OrderCompletionRepository {
        <<interface>>
        +save(OrderCompletion completion) OrderCompletion
        +findByPaymentOrderId(Long orderId) Optional~OrderCompletion~
        +existsByPaymentId(Long paymentId) boolean
    }
    class PaymentRepository {
        <<interface>>
        +findByOrderId(Long orderId) Optional~Payment~
        +save(Payment payment) Payment
    }
    class OrderRepository {
        <<interface>>
        +findById(Long id) Optional~Order~
        +save(Order order) Order
    }
    class OrderCompletion {
        -Long id
        -Payment payment
        -User confirmedBy
        -LocalDateTime confirmedAt
        -String completionStatus
        -String evidenceImageUrl
    }
    class Payment {
        -Long id
        -Order order
        -String paymentStatus
    }
    class Order {
        -Long id
        -String status
    }
    class UserRepository {
        <<interface>>
        +findById(Long id) Optional~User~
    }
    class User {
        -Long id
        -String fullName
    }
    class ConfirmReceiptRequestDTO
    class CompletionResponseDTO
    class ApiResponseDTO~T~ {
        -String status
        -String message
        -T data
        -List~FieldErrorDTO~ errors
    }

    OrderCompletionController --> OrderCompletionService : uses
    OrderCompletionController ..> ApiResponseDTO : returns
    OrderCompletionServiceImpl ..|> OrderCompletionService : implements
    OrderCompletionServiceImpl --> OrderCompletionRepository : uses
    OrderCompletionServiceImpl --> PaymentRepository : uses
    OrderCompletionServiceImpl --> OrderRepository : uses
    OrderCompletionServiceImpl --> UserRepository : uses
    OrderCompletionServiceImpl --> FileStorageService : uses
    OrderCompletionRepository ..> OrderCompletion : manages
    PaymentRepository ..> Payment : manages
    OrderRepository ..> Order : manages
    UserRepository ..> User : manages
    OrderCompletion "1" --> "1" Payment : payment
    OrderCompletion --> User : confirmedBy
    Payment "1" --> "1" Order : order
    OrderCompletionServiceImpl ..> ConfirmReceiptRequestDTO : accepts
    OrderCompletionServiceImpl ..> CompletionResponseDTO : creates
    ApiResponseDTO ..> CompletionResponseDTO : wraps
```

## Sequence Diagrams

```mermaid
sequenceDiagram
    autonumber
    actor Buyer as User (BUYER / RUNNER)
    participant Ctrl as OrderCompletionController<br/>(/api/v1/orders/{orderId}/completion)
    participant Svc as OrderCompletionServiceImpl
    participant OrderRepo as OrderRepository
    participant PayRepo as PaymentRepository
    participant CompRepo as OrderCompletionRepository
    participant UserRepo as UserRepository
    participant FileSvc as FileStorageService
    participant DB as PostgreSQL Database (ORDER_COMPLETIONS, PAYMENTS, ORDERS)

    %% ==========================================
    %% 1. Endpoint 1: POST /api/v1/orders/{orderId}/completion (Confirm Receipt and Close Order)
    %% ==========================================
    rect rgb(240, 255, 240)
    note over Buyer, DB: 1. Endpoint 1: Confirm Receipt and Close Order (POST /api/v1/orders/{orderId}/completion)
    Buyer->>Ctrl: POST /api/v1/orders/{orderId}/completion (ConfirmReceiptRequestDTO)
    activate Ctrl
    note right of Buyer: Header: Bearer <JWT_TOKEN> (ผู้ฝากซื้อเจ้าของออเดอร์เท่านั้น)<br/>Content-Type: multipart/form-data<br/>Parts: evidenceImage (file <= 5MB, JPEG/PNG)

    alt Missing/Invalid Token
        Ctrl-->>Buyer: 401 Unauthorized (Invalid or missing token)
    else Validation Failed (File missing / > 5MB / Not JPEG or PNG)
        Ctrl-->>Buyer: 400 Bad Request (ApiResponseDTO with FieldErrorDTO)
    else Authorization & Validation Passed
        Ctrl->>Svc: confirmReceipt(buyerId, orderId, requestDTO)
        activate Svc

        Svc->>OrderRepo: findById(orderId)
        activate OrderRepo
        OrderRepo->>DB: SELECT * FROM ORDERS WHERE id = orderId
        activate DB
        DB-->>OrderRepo: Order Entity / null
        deactivate DB
        OrderRepo-->>Svc: Optional<Order>
        deactivate OrderRepo

        alt Order Not Found
            Svc-->>Ctrl: throw ResourceNotFoundException
            Ctrl-->>Buyer: 404 Not Found (Order not found)
        else User is NOT the buyer of this order
            Svc-->>Ctrl: throw AccessDeniedException
            Ctrl-->>Buyer: 403 Forbidden (You are not the owner of this order)
        else Order status is 'PENDING' (ยังไม่ชำระเงิน)
            Svc-->>Ctrl: throw IllegalStateException
            Ctrl-->>Buyer: 409 Conflict (Order has not been paid)
        else Order status is 'COMPLETED' (ปิดออเดอร์ไปแล้ว)
            Svc-->>Ctrl: throw IllegalStateException
            Ctrl-->>Buyer: 409 Conflict (Order is already completed)
        else Order Status is 'PAID' & Valid
            Svc->>PayRepo: findByOrderId(orderId)
            activate PayRepo
            PayRepo->>DB: SELECT * FROM PAYMENTS WHERE order_id = orderId
            activate DB
            DB-->>PayRepo: Payment Entity / null
            deactivate DB
            PayRepo-->>Svc: Optional<Payment>
            deactivate PayRepo

            Svc->>UserRepo: findById(buyerId)
            activate UserRepo
            UserRepo->>DB: SELECT * FROM USERS WHERE id = buyerId
            activate DB
            DB-->>UserRepo: User Entity (Buyer)
            deactivate DB
            UserRepo-->>Svc: User
            deactivate UserRepo

            %% Single Transaction Execution
            note over Svc, DB: Atomic Transaction (ถ้าขั้นตอนใดล้มเหลว Rollback ทั้งหมด)

            %% Step 1: Save Evidence Image File
            Svc->>FileSvc: store(evidenceImage, "completions")
            activate FileSvc
            FileSvc-->>Svc: evidenceImageUrl (e.g. "/uploads/completions/5001-1761376200.jpg")
            deactivate FileSvc

            %% Step 2: Create & Save OrderCompletion Entity
            Svc->>Svc: Construct OrderCompletion entity<br/>(payment, confirmedBy=buyer, confirmedAt=NOW(), completionStatus='COMPLETED', evidenceImageUrl)
            Svc->>CompRepo: save(orderCompletion)
            activate CompRepo
            CompRepo->>DB: INSERT INTO ORDER_COMPLETIONS (payment_id, confirmed_by, confirmed_at, completion_status, evidence_image_url) VALUES (...)
            activate DB
            DB-->>CompRepo: Saved OrderCompletion (with generated completionId)
            deactivate DB
            CompRepo-->>Svc: OrderCompletion Entity
            deactivate CompRepo

            %% Step 3: Update Payment Status to RELEASED
            Svc->>Svc: Set payment.paymentStatus = 'RELEASED'
            Svc->>PayRepo: save(payment)
            activate PayRepo
            PayRepo->>DB: UPDATE PAYMENTS SET payment_status = 'RELEASED' WHERE id = ?
            activate DB
            DB-->>PayRepo: Updated Payment
            deactivate DB
            PayRepo-->>Svc: Payment Entity
            deactivate PayRepo

            %% Step 4: Update Order Status to COMPLETED
            Svc->>Svc: Set order.status = 'COMPLETED'
            Svc->>OrderRepo: save(order)
            activate OrderRepo
            OrderRepo->>DB: UPDATE ORDERS SET status = 'COMPLETED' WHERE id = ?
            activate DB
            DB-->>OrderRepo: Updated Order
            deactivate DB
            OrderRepo-->>Svc: Order Entity
            deactivate OrderRepo

            Svc-->>Ctrl: CompletionResponseDTO
            deactivate Svc
            Ctrl-->>Buyer: 201 Created<br/>ApiResponseDTO(status="SUCCESS", message="Order completed successfully", data=CompletionResponseDTO)
            deactivate Ctrl
        end
    end
    end

    %% ==========================================
    %% 2. Endpoint 2: GET /api/v1/orders/{orderId}/completion (Get Order Completion Detail)
    %% ==========================================
    rect rgb(240, 248, 255)
    note over Buyer, DB: 2. Endpoint 2: Get Order Completion Detail (GET /api/v1/orders/{orderId}/completion)
    Buyer->>Ctrl: GET /api/v1/orders/{orderId}/completion
    activate Ctrl
    note right of Buyer: Header: Bearer <JWT_TOKEN> (ผู้ฝากซื้อเจ้าของออเดอร์ หรือ ผู้รับหิ้วเจ้าของรอบ)

    alt Missing/Invalid Token
        Ctrl-->>Buyer: 401 Unauthorized
    else Authorized Token Passed
        Ctrl->>Svc: getCompletion(userId, orderId)
        activate Svc

        Svc->>OrderRepo: findById(orderId)
        activate OrderRepo
        OrderRepo->>DB: SELECT * FROM ORDERS WHERE id = orderId
        activate DB
        DB-->>OrderRepo: Order Entity / null
        deactivate DB
        OrderRepo-->>Svc: Optional<Order>
        deactivate OrderRepo

        alt Order Not Found
            Svc-->>Ctrl: throw ResourceNotFoundException
            Ctrl-->>Buyer: 404 Not Found (Order not found)
        else User is NEITHER Buyer NOR Runner of this order
            Svc-->>Ctrl: throw AccessDeniedException
            Ctrl-->>Buyer: 403 Forbidden (You are not allowed to view this order)
        else User Authorized
            Svc->>PayRepo: findByOrderId(orderId)
            activate PayRepo
            PayRepo->>DB: SELECT * FROM PAYMENTS WHERE order_id = orderId
            activate DB
            DB-->>PayRepo: Payment Entity / null
            deactivate DB
            PayRepo-->>Svc: Optional<Payment>
            deactivate PayRepo

            alt Payment Not Found
                Svc-->>Ctrl: throw ResourceNotFoundException
                Ctrl-->>Buyer: 404 Not Found (Order completion not found)
            else Payment Found
                Svc->>CompRepo: findByPaymentId(payment.id)
                activate CompRepo
                CompRepo->>DB: SELECT * FROM ORDER_COMPLETIONS WHERE payment_id = payment.id
                activate DB
                DB-->>CompRepo: OrderCompletion Entity / null
                deactivate DB
                CompRepo-->>Svc: Optional<OrderCompletion>
                deactivate CompRepo

                alt Order Completion Record Not Found
                    Svc-->>Ctrl: throw ResourceNotFoundException
                    Ctrl-->>Runner: 404 Not Found (Order completion not found)
                else Record Found
                    Svc-->>Ctrl: CompletionResponseDTO
                    deactivate Svc
                    Ctrl-->>Buyer: 200 OK<br/>ApiResponseDTO(status="SUCCESS", data=CompletionResponseDTO)
                    deactivate Ctrl
                end
            end
        end
    end
<<<<<<< HEAD
    end
=======
    end
```

## 6. Static HTML integration

| UI surface | Task 24 endpoint |
|---|---|
| `order-detail.html?orderId=...` links to `delivery-proof.html?orderId=...` | Opens the order's receipt-confirmation page |
| `delivery-proof.html?orderId=...` | `GET /api/v1/orders/{orderId}` and `GET /api/v1/orders/{orderId}/completion` |
| Buyer submits receipt evidence on `delivery-proof.html` | `POST /api/v1/orders/{orderId}/completion` (multipart `evidenceImage`) |

The buyer can submit a JPEG/PNG image up to 5 MB only after payment. The page displays the completion result, evidence link, released amount, and released payment/order statuses from the API.
*** End of File
>>>>>>> 2d2b5f35d2922118fc92063e91ef5fb8645b1a8f
