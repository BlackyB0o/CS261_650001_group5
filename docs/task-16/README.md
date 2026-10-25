# Task16: US03 - ฝากซื้อสินค้า/อาหาร (API, API Class Diagram, DTO, DAO)

> ใช้ร่วมกับ [API Conventions](../common/API-CONVENTIONS.md) (รูปแบบ response, JWT, enum) และ [ERD](../database/ERD.md) (ตาราง `ORDERS`, `ORDER_ITEMS`)
> ต่อจาก Task04 (ผู้ใช้) และ Task08 (รอบหิ้ว) ผู้ฝากหิ้วต้องเลือกรอบที่ `OPEN` ก่อนจึงสร้างออเดอร์ได้

## 0. เงื่อนไขจาก User Story ที่ API ต้องรองรับ

| Test case ใน US03 | สิ่งที่ API ต้องทำ | Response |
|---|---|---|
| 1. ผู้ฝากหิ้วกรอกรายการสินค้า/อาหารและจุดนัดรับครบ ระบบบันทึกรายการฝากซื้อได้ | สร้างออเดอร์สถานะ `PENDING` พร้อมรายการสินค้า | `201 Created` |
| 2. กรอกรายการสินค้า/อาหารหรือจุดนัดรับไม่ครบ ระบบไม่อนุญาตให้ฝากซื้อ | ไม่บันทึก และบอกว่าช่องไหนขาด | `400 Bad Request` |
| So that: ผู้รับหิ้วทราบรายละเอียดสินค้าที่ต้องซื้ออย่างชัดเจน | ให้ผู้รับหิ้วดูออเดอร์ทั้งหมดในรอบของตน | Endpoint 3 |

---

## 1. API Specification

### Endpoint 1: Create Order (ฝากซื้อสินค้า/อาหาร)
- **URL:** `/api/v1/orders`
- **Method:** `POST`
- **Headers:** `Authorization: Bearer <JWT_TOKEN>` (ต้องเข้าสู่ระบบ)
- **Content-Type:** `application/json`
- **Request Body:**
```json
{
  "roundId": 101,
  "handoffMethod": "MEETUP",
  "handoffLocation": "หน้าอาคารเรียนรวมสังคมศาสตร์ (SC1) มธ.รังสิต",
  "items": [
    {
      "productName": "Iced Americano (Venti)",
      "quantity": 2,
      "note": "หวานน้อย"
    },
    {
      "productName": "Butter Croissant",
      "quantity": 1,
      "note": null
    }
  ]
}
```
*(หมายเหตุ: `handoffMethod` เลือกได้ `"MEETUP"` (นัดรับ) หรือ `"DELIVERY"` (ส่งถึงที่) และ `buyerId` ไม่ต้องส่งมา ระบบอ่านจาก JWT)*

**กติกาตรวจข้อมูล**

| Field | กติกา | ตัวอย่าง message |
|---|---|---|
| `roundId` | ห้ามว่าง | `must not be null` |
| `handoffMethod` | ห้ามว่าง และต้องเป็น `MEETUP` หรือ `DELIVERY` | `must be MEETUP or DELIVERY` |
| `handoffLocation` | ห้ามว่าง | `must not be blank` |
| `items` | ต้องมีอย่างน้อย 1 รายการ | `must not be empty` |
| `items[i].productName` | ห้ามว่าง | `must not be blank` |
| `items[i].quantity` | ห้ามว่าง และต้อง ≥ 1 | `must be greater than or equal to 1` |
| `items[i].note` | ไม่บังคับ | - |

- **Response (201 Created):**
```json
{
  "status": "SUCCESS",
  "message": "Order created successfully",
  "data": {
    "orderId": 5001,
    "roundId": 101,
    "buyerId": 2,
    "buyerName": "Somying Rakdee",
    "handoffMethod": "MEETUP",
    "handoffLocation": "หน้าอาคารเรียนรวมสังคมศาสตร์ (SC1) มธ.รังสิต",
    "status": "PENDING",
    "createdAt": "2026-10-15T21:30:00",
    "items": [
      {
        "itemId": 9001,
        "productName": "Iced Americano (Venti)",
        "quantity": 2,
        "note": "หวานน้อย"
      },
      {
        "itemId": 9002,
        "productName": "Butter Croissant",
        "quantity": 1,
        "note": null
      }
    ]
  }
}
```

**Error Responses**

| HTTP Status | เงื่อนไข | ตัวอย่าง message |
|---|---|---|
| `400 Bad Request` | ข้อมูลไม่ครบ/ไม่ถูกต้อง เช่น `items` ว่าง, `quantity` น้อยกว่า 1, `handoffMethod` ไม่อยู่ในค่าที่กำหนด | `Validation failed` (พร้อมรายการ field ที่ผิดใน `errors`) |
| `401 Unauthorized` | ไม่ส่ง token หรือ token ไม่ถูกต้อง/หมดอายุ | `Invalid or missing token` |
| `403 Forbidden` | ฝากซื้อในรอบหิ้วของตัวเอง | `You cannot order from your own round` |
| `404 Not Found` | ไม่พบรอบหิ้วตาม `roundId` | `Delivery round not found` |
| `409 Conflict` | รอบหิ้วไม่ได้อยู่ในสถานะ `OPEN` | `Delivery round is not open` |

ตัวอย่าง `400`:
```json
{
  "status": "ERROR",
  "message": "Validation failed",
  "errors": [
    { "field": "handoffLocation", "message": "must not be blank" },
    { "field": "items[0].quantity", "message": "must be greater than or equal to 1" }
  ]
}
```

### Endpoint 2: Get My Orders (รายการสั่งซื้อของฉัน)
- **URL:** `/api/v1/orders/my`
- **Method:** `GET`
- **Headers:** `Authorization: Bearer <JWT_TOKEN>` (ต้องเข้าสู่ระบบ)
- **ผลลัพธ์:** ออเดอร์ทั้งหมดของผู้ฝากหิ้วที่ล็อกอินอยู่ ทุกสถานะ เรียงจาก `createdAt` ล่าสุดไปเก่าสุด
- **Response (200 OK):** `data` เป็น array ของออเดอร์ รูปแบบเดียวกับ `data` ใน Endpoint 1
- **Error:** `401` เมื่อไม่มี token

### Endpoint 3: Get Orders of a Round (ออเดอร์ทั้งหมดในรอบของผู้รับหิ้ว)
- **URL:** `/api/v1/orders?roundId=101`
- **Method:** `GET`
- **Headers:** `Authorization: Bearer <JWT_TOKEN>` (เจ้าของรอบนั้นเท่านั้น)
- **Query Parameter:** `roundId` (บังคับ)
- **ผลลัพธ์:** ออเดอร์ทุกสถานะในรอบนั้น พร้อมรายการสินค้า เรียงจาก `createdAt` เก่าไปใหม่
- **Response (200 OK):** `data` เป็น array ของออเดอร์ รูปแบบเดียวกับ `data` ใน Endpoint 1
- **Error:**

| HTTP Status | เงื่อนไข | ตัวอย่าง message |
|---|---|---|
| `400 Bad Request` | ไม่ส่ง `roundId` | `roundId is required` |
| `401 Unauthorized` | ไม่มี token | `Invalid or missing token` |
| `403 Forbidden` | ไม่ใช่เจ้าของรอบนี้ | `You are not the owner of this delivery round` |
| `404 Not Found` | ไม่พบรอบ | `Delivery round not found` |

### Endpoint 4: Get Order Detail (รายละเอียดออเดอร์)
- **URL:** `/api/v1/orders/{orderId}`
- **Method:** `GET`
- **Headers:** `Authorization: Bearer <JWT_TOKEN>` (ผู้ฝากหิ้วเจ้าของออเดอร์ หรือผู้รับหิ้วเจ้าของรอบที่ออเดอร์อยู่)
- **Response (200 OK):** `data` เป็นออเดอร์เดียว รูปแบบเดียวกับ `data` ใน Endpoint 1
- **Error:** `401` เมื่อไม่มี token, `403` (`You are not allowed to view this order`) เมื่อไม่ใช่ผู้เกี่ยวข้อง, `404` (`Order not found`) เมื่อไม่พบออเดอร์

---

## 2. API Class Diagram
```mermaid
classDiagram
    class OrderController {
        +createOrder(CreateOrderRequestDTO request) ResponseEntity~ApiResponseDTO~
        +getMyOrders() ResponseEntity~ApiResponseDTO~
        +getOrdersByRound(Long roundId) ResponseEntity~ApiResponseDTO~
        +getOrder(Long orderId) ResponseEntity~ApiResponseDTO~
    }
    class CreateOrderRequestDTO {
        +Long roundId
        +String handoffMethod
        +String handoffLocation
        +List~OrderItemRequestDTO~ items
    }
    class OrderItemRequestDTO {
        +String productName
        +Integer quantity
        +String note
    }
    class OrderResponseDTO {
        +Long orderId
        +Long roundId
        +Long buyerId
        +String buyerName
        +String handoffMethod
        +String handoffLocation
        +String status
        +LocalDateTime createdAt
        +List~OrderItemResponseDTO~ items
    }
    class OrderItemResponseDTO {
        +Long itemId
        +String productName
        +Integer quantity
        +String note
    }
    class ApiResponseDTO~T~ {
        +String status
        +String message
        +T data
    }

    OrderController ..> CreateOrderRequestDTO : receives
    OrderController ..> ApiResponseDTO : returns
    ApiResponseDTO ..> OrderResponseDTO : wraps
    CreateOrderRequestDTO o-- OrderItemRequestDTO : items
    OrderResponseDTO o-- OrderItemResponseDTO : items
```

---

## 3. DTO Class Specification & Diagram

`ApiResponseDTO<T>` และ `FieldErrorDTO` ใช้ร่วมกันทุก task ดูที่ [API Conventions](../common/API-CONVENTIONS.md)

### DTO Specifications
1. **`CreateOrderRequestDTO`**
   - `roundId`: Long (Not Null)
   - `handoffMethod`: String (Not Null, Choices: `"MEETUP"`, `"DELIVERY"`)
   - `handoffLocation`: String (Not Blank)
   - `items`: List&lt;OrderItemRequestDTO&gt; (Not Empty, Valid)
2. **`OrderItemRequestDTO`**
   - `productName`: String (Not Blank)
   - `quantity`: Integer (Not Null, Min 1)
   - `note`: String (Optional)
3. **`OrderResponseDTO`**
   - `orderId`: Long
   - `roundId`: Long
   - `buyerId`: Long
   - `buyerName`: String (ดึงจาก `USERS.full_name` ของผู้ฝากหิ้ว ให้ผู้รับหิ้วรู้ว่าออเดอร์ของใคร)
   - `handoffMethod`: String
   - `handoffLocation`: String
   - `status`: String (`PENDING` เมื่อสร้างใหม่)
   - `createdAt`: LocalDateTime
   - `items`: List&lt;OrderItemResponseDTO&gt;
4. **`OrderItemResponseDTO`**
   - `itemId`: Long
   - `productName`: String
   - `quantity`: Integer
   - `note`: String

### DTO Class Diagram
```mermaid
classDiagram
    class CreateOrderRequestDTO {
        -Long roundId
        -String handoffMethod
        -String handoffLocation
        -List~OrderItemRequestDTO~ items
        +getRoundId() Long
        +getHandoffMethod() String
        +getHandoffLocation() String
        +getItems() List~OrderItemRequestDTO~
    }

    class OrderItemRequestDTO {
        -String productName
        -Integer quantity
        -String note
        +getProductName() String
        +getQuantity() Integer
        +getNote() String
    }

    class OrderResponseDTO {
        -Long orderId
        -Long roundId
        -Long buyerId
        -String buyerName
        -String handoffMethod
        -String handoffLocation
        -String status
        -LocalDateTime createdAt
        -List~OrderItemResponseDTO~ items
        +getOrderId() Long
        +getRoundId() Long
        +getBuyerId() Long
        +getBuyerName() String
        +getHandoffMethod() String
        +getHandoffLocation() String
        +getStatus() String
        +getCreatedAt() LocalDateTime
        +getItems() List~OrderItemResponseDTO~
    }

    class OrderItemResponseDTO {
        -Long itemId
        -String productName
        -Integer quantity
        -String note
        +getItemId() Long
        +getProductName() String
        +getQuantity() Integer
        +getNote() String
    }

    CreateOrderRequestDTO "1" o-- "1..*" OrderItemRequestDTO : items
    OrderResponseDTO "1" o-- "1..*" OrderItemResponseDTO : items
```

---

## 4. DAO / Repository Specification & Diagram

### Entity ที่เกี่ยวข้อง

**`Order`** (ตาราง `orders` ต้องระบุ `@Table(name = "orders")`)

| Field | Column ใน ERD | หมายเหตุ |
|---|---|---|
| `id` | `id` | PK |
| `buyer` (`User`) | `buyer_id` | FK → `USERS.id` (`@ManyToOne`) |
| `round` (`DeliveryRound`) | `round_id` | FK → `DELIVERY_ROUNDS.id` (`@ManyToOne`) |
| `handoffMethod` | `handoff_method` | NOT NULL (`MEETUP` / `DELIVERY`) |
| `handoffLocation` | `handoff_location` | NOT NULL |
| `status` | `status` | NOT NULL (`PENDING` / `PAID` / `COMPLETED`) |
| `createdAt` | `created_at` | NOT NULL, DEFAULT CURRENT_TIMESTAMP |
| `items` | - | `@OneToMany(mappedBy = "order", cascade = ALL)` บันทึกรายการสินค้าพร้อมออเดอร์ |

**`OrderItem`** (ตาราง `order_items`)

| Field | Column ใน ERD | หมายเหตุ |
|---|---|---|
| `id` | `id` | PK |
| `order` (`Order`) | `order_id` | FK → `ORDERS.id` |
| `productName` | `product_name` | NOT NULL |
| `quantity` | `quantity` | NOT NULL, > 0 |
| `note` | `note` | ไม่บังคับ |

### Repository
- **Interface:** `OrderRepository` (extends `JpaRepository<Order, Long>`)
- **Methods:**
  - `Order save(Order order)` - บันทึกออเดอร์พร้อมรายการสินค้า และอัปเดตสถานะ (Task20, Task24 ใช้ตอนเปลี่ยนเป็น `PAID` / `COMPLETED`)
  - `Optional<Order> findById(Long id)` - ค้นหาออเดอร์ด้วย id (Endpoint 4)
  - `List<Order> findByBuyerIdOrderByCreatedAtDesc(Long buyerId)` - ออเดอร์ของผู้ฝากหิ้ว (Endpoint 2)
  - `List<Order> findByRoundIdOrderByCreatedAtAsc(Long roundId)` - ออเดอร์ทั้งหมดในรอบ (Endpoint 3)
- ไม่ต้องมี repository แยกของ `OrderItem` เพราะบันทึกผ่าน `Order` (cascade)
- **ใช้ร่วมกับ task อื่น:** `DeliveryRoundRepository.findById` (Task08) ตรวจว่ารอบมีอยู่และเป็น `OPEN` ก่อนสร้างออเดอร์ และตรวจเจ้าของรอบใน Endpoint 3, `UserRepository.findById` (Task04) ดึงผู้ฝากหิ้วจาก JWT

```mermaid
classDiagram
    class OrderRepository {
        <<interface>>
        +save(Order order) Order
        +findById(Long id) Optional~Order~
        +findByBuyerIdOrderByCreatedAtDesc(Long buyerId) List~Order~
        +findByRoundIdOrderByCreatedAtAsc(Long roundId) List~Order~
    }
    class Order {
        -Long id
        -User buyer
        -DeliveryRound round
        -String handoffMethod
        -String handoffLocation
        -String status
        -LocalDateTime createdAt
        -List~OrderItem~ items
    }
    class OrderItem {
        -Long id
        -Order order
        -String productName
        -Integer quantity
        -String note
    }
    class User {
        -Long id
        -String fullName
    }
    class DeliveryRound {
        -Long id
        -String status
    }

    OrderRepository ..> Order : manages
    Order "1" o-- "1..*" OrderItem : items
    Order --> User : buyer
    Order --> DeliveryRound : round
```

---

## 5. Extended API Class Diagram (Full Layered Architecture)

Controller เรียก Service, Service ใช้ `OrderRepository` และ `DeliveryRoundRepository` (Task08) เพื่อตรวจว่ารอบยัง `OPEN`

```mermaid
classDiagram
    class OrderController {
        -OrderService orderService
        +createOrder(CreateOrderRequestDTO request) ResponseEntity~ApiResponseDTO~
        +getMyOrders() ResponseEntity~ApiResponseDTO~
        +getOrdersByRound(Long roundId) ResponseEntity~ApiResponseDTO~
        +getOrder(Long orderId) ResponseEntity~ApiResponseDTO~
    }
    class OrderService {
        <<interface>>
        +createOrder(Long buyerId, CreateOrderRequestDTO request) OrderResponseDTO
        +getMyOrders(Long buyerId) List~OrderResponseDTO~
        +getOrdersByRound(Long runnerId, Long roundId) List~OrderResponseDTO~
        +getOrder(Long userId, Long orderId) OrderResponseDTO
    }
    class OrderServiceImpl {
        -OrderRepository orderRepository
        -DeliveryRoundRepository roundRepository
        -UserRepository userRepository
    }
    class OrderRepository {
        <<interface>>
        +save(Order order) Order
        +findById(Long id) Optional~Order~
        +findByBuyerIdOrderByCreatedAtDesc(Long buyerId) List~Order~
        +findByRoundIdOrderByCreatedAtAsc(Long roundId) List~Order~
    }
    class DeliveryRoundRepository {
        <<interface>>
        +findById(Long id) Optional~DeliveryRound~
    }
    class Order {
        -Long id
        -User buyer
        -DeliveryRound round
        -String handoffMethod
        -String handoffLocation
        -String status
        -LocalDateTime createdAt
        -List~OrderItem~ items
    }
    class OrderItem {
        -Long id
        -String productName
        -Integer quantity
        -String note
    }
    class DeliveryRound {
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
    class CreateOrderRequestDTO
    class OrderItemRequestDTO
    class OrderResponseDTO
    class OrderItemResponseDTO
    class ApiResponseDTO~T~ {
        -String status
        -String message
        -T data
        -List~FieldErrorDTO~ errors
    }

    OrderController --> OrderService : uses
    OrderController ..> ApiResponseDTO : returns
    OrderServiceImpl ..|> OrderService : implements
    OrderServiceImpl --> OrderRepository : uses
    OrderServiceImpl --> DeliveryRoundRepository : uses
    OrderServiceImpl --> UserRepository : uses
    OrderRepository ..> Order : manages
    DeliveryRoundRepository ..> DeliveryRound : manages
    UserRepository ..> User : manages
    Order "1" --> "1..*" OrderItem : items
    Order --> User : buyer
    Order --> DeliveryRound : round
    OrderServiceImpl ..> CreateOrderRequestDTO : accepts
    CreateOrderRequestDTO "1" --> "1..*" OrderItemRequestDTO : items
    OrderServiceImpl ..> OrderResponseDTO : creates
    OrderResponseDTO "1" --> "1..*" OrderItemResponseDTO : items
    ApiResponseDTO ..> OrderResponseDTO : wraps
```

## Sequence Diagrams

```mermaid
sequenceDiagram
    autonumber
    actor Buyer as User (BUYER / RUNNER)
    participant Ctrl as OrderController<br/>(/api/v1/orders)
    participant Svc as OrderServiceImpl
    participant UserRepo as UserRepository
    participant RoundRepo as DeliveryRoundRepository
    participant OrderRepo as OrderRepository
    participant DB as PostgreSQL Database (ORDERS & ORDER_ITEMS)

    %% ==========================================
    %% 1. Endpoint 1: POST /api/v1/orders (Create Order)
    %% ==========================================
    rect rgb(240, 255, 240)
    note over Buyer, DB: 1. Endpoint 1: Create Order (POST /api/v1/orders)
    Buyer->>Ctrl: POST /api/v1/orders (CreateOrderRequestDTO)
    activate Ctrl
    note right of Buyer: Header: Bearer <JWT_TOKEN> (ต้องเข้าสู่ระบบ)<br/>Body: roundId, handoffMethod, handoffLocation, items[]

    alt Missing/Invalid Token
        Ctrl-->>Buyer: 401 Unauthorized (Invalid or missing token)
    else Validation Failed (roundId/handoff null, items empty, quantity < 1)
        Ctrl-->>Buyer: 400 Bad Request (ApiResponseDTO with FieldErrorDTO)
    else Authorization & Validation Passed
        Ctrl->>Svc: createOrder(buyerId, requestDTO)
        activate Svc

        %% Step 1: Validate Delivery Round
        Svc->>RoundRepo: findById(roundId)
        activate RoundRepo
        RoundRepo->>DB: SELECT * FROM DELIVERY_ROUNDS WHERE id = roundId
        activate DB
        DB-->>RoundRepo: DeliveryRound Entity / null
        deactivate DB
        RoundRepo-->>Svc: Optional<DeliveryRound>
        deactivate RoundRepo

        alt Delivery Round Not Found
            Svc-->>Ctrl: throw ResourceNotFoundException
            Ctrl-->>Buyer: 404 Not Found (Delivery round not found)
        else User is the Runner of this Delivery Round (buyerId == runnerId)
            Svc-->>Ctrl: throw AccessDeniedException
            Ctrl-->>Buyer: 403 Forbidden (You cannot order from your own round)
        else Delivery Round status is NOT 'OPEN'
            Svc-->>Ctrl: throw IllegalStateException
            Ctrl-->>Buyer: 409 Conflict (Delivery round is not open)
        else Delivery Round is OPEN & Valid
            
            %% Step 2: Fetch Buyer Info
            Svc->>UserRepo: findById(buyerId)
            activate UserRepo
            UserRepo->>DB: SELECT * FROM USERS WHERE id = buyerId
            activate DB
            DB-->>UserRepo: User Entity (Buyer)
            deactivate DB
            UserRepo-->>Svc: User
            deactivate UserRepo

            %% Step 3: Create Order & OrderItems (Cascade All)
            Svc->>Svc: Construct Order entity (status = 'PENDING')<br/>Construct OrderItem entities mapped to Order
            
            Svc->>OrderRepo: save(order)
            activate OrderRepo
            OrderRepo->>DB: INSERT INTO ORDERS (buyer_id, round_id, handoff_method, handoff_location, status) VALUES (...)
            activate DB
            DB-->>OrderRepo: Saved Order (with generated orderId)
            deactivate DB
            
            OrderRepo->>DB: INSERT INTO ORDER_ITEMS (order_id, product_name, quantity, note) VALUES (...)
            activate DB
            DB-->>OrderRepo: Saved OrderItems
            deactivate DB
            
            OrderRepo-->>Svc: Order Entity (with items)
            deactivate OrderRepo

            Svc-->>Ctrl: OrderResponseDTO
            deactivate Svc
            Ctrl-->>Buyer: 201 Created<br/>ApiResponseDTO(status="SUCCESS", message="Order created successfully", data=OrderResponseDTO)
            deactivate Ctrl
        end
    end
<<<<<<< HEAD
=======

>>>>>>> 2d2b5f35d2922118fc92063e91ef5fb8645b1a8f
    end

    %% ==========================================
    %% 2. Endpoint 2: GET /api/v1/orders/my (Get My Orders)
    %% ==========================================
    rect rgb(240, 248, 255)
    note over Buyer, DB: 2. Endpoint 2: Get My Orders (GET /api/v1/orders/my)
    Buyer->>Ctrl: GET /api/v1/orders/my
    activate Ctrl
    note right of Buyer: Header: Bearer <JWT_TOKEN> (ต้องเข้าสู่ระบบ)

    alt Missing/Invalid Token
        Ctrl-->>Buyer: 401 Unauthorized
    else Authorized
        Ctrl->>Svc: getMyOrders(buyerId)
        activate Svc
        
        Svc->>OrderRepo: findByBuyerIdOrderByCreatedAtDesc(buyerId)
        activate OrderRepo
        OrderRepo->>DB: SELECT * FROM ORDERS WHERE buyer_id = ? ORDER BY created_at DESC
        activate DB
        DB-->>OrderRepo: List<Order> (with items)
        deactivate DB
        OrderRepo-->>Svc: List<Order>
        deactivate OrderRepo

        Svc-->>Ctrl: List<OrderResponseDTO>
        deactivate Svc
        Ctrl-->>Buyer: 200 OK<br/>ApiResponseDTO(status="SUCCESS", data=List<OrderResponseDTO>)
        deactivate Ctrl
    end
    end

    %% ==========================================
    %% 3. Endpoint 3: GET /api/v1/orders?roundId={roundId} (Get Orders of a Round)
    %% ==========================================
    rect rgb(255, 250, 240)
    note over Buyer, DB: 3. Endpoint 3: Get Orders of a Round (GET /api/v1/orders?roundId=101)
    Buyer->>Ctrl: GET /api/v1/orders?roundId={roundId}
    activate Ctrl
    note right of Buyer: Header: Bearer <JWT_TOKEN> (ผู้หิ้วเจ้าของรอบนั้นๆ เท่านั้น)

    alt Missing roundId Query Parameter
        Ctrl-->>Buyer: 400 Bad Request (roundId is required)
    else Missing/Invalid Token
        Ctrl-->>Buyer: 401 Unauthorized
    else Request Valid
        Ctrl->>Svc: getOrdersByRound(runnerId, roundId)
        activate Svc

        Svc->>RoundRepo: findById(roundId)
        activate RoundRepo
        RoundRepo->>DB: SELECT * FROM DELIVERY_ROUNDS WHERE id = roundId
        activate DB
        DB-->>RoundRepo: DeliveryRound Entity / null
        deactivate DB
        RoundRepo-->>Svc: Optional<DeliveryRound>
        deactivate RoundRepo

        alt Delivery Round Not Found
            Svc-->>Ctrl: throw ResourceNotFoundException
            Ctrl-->>Buyer: 404 Not Found (Delivery round not found)
        else User is NOT the runner of this Delivery Round
            Svc-->>Ctrl: throw AccessDeniedException
            Ctrl-->>Buyer: 403 Forbidden (You are not the owner of this delivery round)
        else User is the Runner
            Svc->>OrderRepo: findByRoundIdOrderByCreatedAtAsc(roundId)
            activate OrderRepo
            OrderRepo->>DB: SELECT * FROM ORDERS WHERE round_id = ? ORDER BY created_at ASC
            activate DB
            DB-->>OrderRepo: List<Order> (with items)
            deactivate DB
            OrderRepo-->>Svc: List<Order>
            deactivate OrderRepo

            Svc-->>Ctrl: List<OrderResponseDTO>
            deactivate Svc
            Ctrl-->>Buyer: 200 OK<br/>ApiResponseDTO(status="SUCCESS", data=List<OrderResponseDTO>)
<<<<<<< HEAD
            deactivate Ctrl
        end
    end
    end

    %% ==========================================
=======
                    deactivate Ctrl
                end
            end
            end

            %% ==========================================
>>>>>>> 2d2b5f35d2922118fc92063e91ef5fb8645b1a8f
    %% 4. Endpoint 4: GET /api/v1/orders/{orderId} (Get Order Detail)
    %% ==========================================
    rect rgb(245, 245, 245)
    note over Buyer, DB: 4. Endpoint 4: Get Order Detail (GET /api/v1/orders/{orderId})
    Buyer->>Ctrl: GET /api/v1/orders/{orderId}
    activate Ctrl
    note right of Buyer: Header: Bearer <JWT_TOKEN> (ผู้ฝากซื้อเจ้าของออเดอร์ หรือ ผู้รับหิ้วเจ้าของรอบ)

    alt Missing/Invalid Token
        Ctrl-->>Buyer: 401 Unauthorized
    else Authorized Token Passed
        Ctrl->>Svc: getOrder(userId, orderId)
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
        else User is NEITHER the Buyer of this order NOR the Runner of the round
            Svc-->>Ctrl: throw AccessDeniedException
            Ctrl-->>Buyer: 403 Forbidden (You are not allowed to view this order)
        else User Authorized
            Svc-->>Ctrl: OrderResponseDTO
            deactivate Svc
            Ctrl-->>Buyer: 200 OK<br/>ApiResponseDTO(status="SUCCESS", data=OrderResponseDTO)
            deactivate Ctrl
        end
    end
<<<<<<< HEAD
    end
=======
    end
```

## 6. Static HTML integration

| UI surface | Task 16 endpoint |
|---|---|
| `create-order.html` (opened from an OPEN round detail) | `POST /api/v1/orders` |
| `orders.html#round-orders-panel` | `GET /api/v1/orders/my` |
| `my-round-detailod.html?roundId=...` | `GET /api/v1/orders?roundId=...` (owner only) |
| `order-detail.html?orderId=...` | `GET /api/v1/orders/{orderId}` (buyer or round owner) |

All order operations use the logged-in user's bearer token. Order IDs and item data are rendered from the API response; the create form submits only the documented request fields.
>>>>>>> 2d2b5f35d2922118fc92063e91ef5fb8645b1a8f
