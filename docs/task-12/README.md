# Task12: US12 - ผู้ฝากหิ้วโพสต์หาคนรับหิ้ว (API, API Class Diagram, DTO, DAO)

> ใช้ร่วมกับ [API Conventions](../common/API-CONVENTIONS.md) และ [ERD](../database/ERD.md) (ตาราง `POSTS`)
> โพสต์เป็นช่องทางแยกจากรอบหิ้ว (Task08) ผู้ใช้โพสต์ได้โดยไม่ต้องรอใครเปิดรอบ และผู้ใช้คนอื่นกด "รับงาน" ได้

## 0. เงื่อนไขจาก User Story ที่ API ต้องรองรับ

| Test case ใน US12 | สิ่งที่ API ต้องทำ | Response |
|---|---|---|
| 1. กรอกรายละเอียดสินค้า ร้านค้า สถานที่ และรายละเอียดการรับสินค้าครบ สร้างโพสต์สำเร็จและผู้รับหิ้วมองเห็นโพสต์เพื่อเลือกติดต่อรับงาน | สร้างโพสต์สถานะ `OPEN` มี endpoint ให้ทุกคนดูโพสต์ และให้ผู้ใช้ที่เข้าสู่ระบบกดรับงานได้ | `201 Created` / Endpoint 2 / Endpoint 5 |
| 2. กรอกข้อมูลที่จำเป็นไม่ครบ ระบบแจ้งเตือนและไม่อนุญาตให้โพสต์ | ไม่บันทึก และบอกว่าช่องไหนขาด | `400 Bad Request` |

**กติกาสิทธิ์:** ไม่มี role ผู้ใช้ทุกคนโพสต์และรับงานได้ Guest (ยังไม่เข้าสู่ระบบ) ดูโพสต์ได้อย่างเดียว

---

## 1. API Specification

### Endpoint 1: Create Post (โพสต์หาคนรับหิ้ว)
- **URL:** `/api/v1/posts`
- **Method:** `POST`
- **Headers:** `Authorization: Bearer <JWT_TOKEN>` (ต้องเข้าสู่ระบบ)
- **Content-Type:** `application/json`
- **Request Body:**
```json
{
  "productDetail": "ชานมไข่มุก 2 แก้ว หวานน้อย",
  "shopName": "ร้านชานมหน้าประตูทางเข้า",
  "location": "ประตูทางเข้ามหาวิทยาลัย",
  "receivingDetail": "รับของที่หน้าตึกวิศวกรรมศาสตร์ ช่วงเที่ยง"
}
```
*(หมายเหตุ: ทั้ง 4 field บังคับส่ง และ `buyerId` ไม่ต้องส่ง ระบบอ่านจาก JWT)*

**กติกาตรวจข้อมูล**

| Field | กติกา | ตัวอย่าง message |
|---|---|---|
| `productDetail` | ห้ามว่าง ไม่เกิน 500 ตัวอักษร | `must not be blank` |
| `shopName` | ห้ามว่าง ไม่เกิน 100 ตัวอักษร | `must not be blank` |
| `location` | ห้ามว่าง ไม่เกิน 255 ตัวอักษร | `must not be blank` |
| `receivingDetail` | ห้ามว่าง ไม่เกิน 500 ตัวอักษร | `must not be blank` |

- **Response (201 Created):**
```json
{
  "status": "SUCCESS",
  "message": "Post created successfully",
  "data": {
    "postId": 1,
    "productDetail": "ชานมไข่มุก 2 แก้ว หวานน้อย",
    "shopName": "ร้านชานมหน้าประตูทางเข้า",
    "location": "ประตูทางเข้ามหาวิทยาลัย",
    "receivingDetail": "รับของที่หน้าตึกวิศวกรรมศาสตร์ ช่วงเที่ยง",
    "status": "OPEN",
    "buyerId": 2,
    "buyerName": "Somchai Jaidee",
    "acceptedBy": null,
    "acceptedByName": null,
    "acceptedAt": null,
    "createdAt": "2026-10-15T20:00:00"
  }
}
```

| HTTP Status | เงื่อนไข | ตัวอย่าง message |
|---|---|---|
| `400 Bad Request` | ข้อมูลไม่ครบหรือไม่ถูกรูปแบบ (ไม่บันทึกโพสต์) | `Validation failed` พร้อม `errors` |
| `401 Unauthorized` | ไม่มี token หรือ token ไม่ถูกต้อง | `Invalid or missing token` |

ตัวอย่าง `400`:
```json
{
  "status": "ERROR",
  "message": "Validation failed",
  "errors": [
    { "field": "productDetail", "message": "must not be blank" },
    { "field": "location", "message": "must not be blank" }
  ]
}
```

### Endpoint 2: Get Open Posts (ดูโพสต์ที่ยังเปิดอยู่)
- **URL:** `/api/v1/posts`
- **Method:** `GET`
- **Headers:** ไม่ต้องใช้ token (Guest ดูได้)
- **Response (200 OK):** โพสต์สถานะ `OPEN` ทั้งหมด เรียงใหม่สุดก่อน `data` เป็น array ของ `PostResponseDTO` (รูปแบบเดียวกับ `data` ของ Endpoint 1)

### Endpoint 3: Get My Posts (โพสต์ของฉัน)
- **URL:** `/api/v1/posts/my`
- **Method:** `GET`
- **Headers:** `Authorization: Bearer <JWT_TOKEN>` (ต้องเข้าสู่ระบบ)
- **Response (200 OK):** โพสต์ทุกสถานะของผู้ใช้ เรียงใหม่สุดก่อน
- **Error:** `401` เมื่อไม่มี token

### Endpoint 4: Get Post Detail (ดูรายละเอียดโพสต์)
- **URL:** `/api/v1/posts/{postId}`
- **Method:** `GET`
- **Headers:** ไม่ต้องใช้ token (Guest ดูได้)
- **Response (200 OK):** `data` เป็น `PostResponseDTO`
- **Error:** `404` (`Post not found`)

### Endpoint 5: Accept Post (กดรับงานโพสต์)
- **URL:** `/api/v1/posts/{postId}/accept`
- **Method:** `PATCH`
- **Headers:** `Authorization: Bearer <JWT_TOKEN>` (ต้องเข้าสู่ระบบ)
- **Request Body:** ไม่มี (ผู้รับงานอ่านจาก JWT)
- **สิ่งที่ระบบทำ:** เปลี่ยนสถานะโพสต์ `OPEN` เป็น `ACCEPTED` และบันทึก `accepted_by`, `accepted_at`
- **Response (200 OK):**
```json
{
  "status": "SUCCESS",
  "message": "Post accepted successfully",
  "data": {
    "postId": 1,
    "productDetail": "ชานมไข่มุก 2 แก้ว หวานน้อย",
    "shopName": "ร้านชานมหน้าประตูทางเข้า",
    "location": "ประตูทางเข้ามหาวิทยาลัย",
    "receivingDetail": "รับของที่หน้าตึกวิศวกรรมศาสตร์ ช่วงเที่ยง",
    "status": "ACCEPTED",
    "buyerId": 2,
    "buyerName": "Somchai Jaidee",
    "acceptedBy": 1,
    "acceptedByName": "Naphat Thongkao",
    "acceptedAt": "2026-10-15T20:30:00",
    "createdAt": "2026-10-15T20:00:00"
  }
}
```

| HTTP Status | เงื่อนไข | ตัวอย่าง message |
|---|---|---|
| `401 Unauthorized` | ไม่มี token | `Invalid or missing token` |
| `403 Forbidden` | รับโพสต์ของตัวเอง | `You cannot accept your own post` |
| `404 Not Found` | ไม่พบโพสต์ | `Post not found` |
| `409 Conflict` | โพสต์ไม่ใช่สถานะ `OPEN` (มีคนรับไปแล้ว) | `Post is no longer open` |

---

## 2. API Class Diagram
```mermaid
classDiagram
    class PostController {
        +createPost(CreatePostRequestDTO request) ResponseEntity~ApiResponseDTO~
        +getOpenPosts() ResponseEntity~ApiResponseDTO~
        +getMyPosts() ResponseEntity~ApiResponseDTO~
        +getPost(Long postId) ResponseEntity~ApiResponseDTO~
        +acceptPost(Long postId) ResponseEntity~ApiResponseDTO~
    }
    class CreatePostRequestDTO {
        +String productDetail
        +String shopName
        +String location
        +String receivingDetail
    }
    class PostResponseDTO {
        +Long postId
        +String productDetail
        +String shopName
        +String location
        +String receivingDetail
        +String status
        +Long buyerId
        +String buyerName
        +Long acceptedBy
        +String acceptedByName
        +LocalDateTime acceptedAt
        +LocalDateTime createdAt
    }
    class ApiResponseDTO~T~ {
        +String status
        +String message
        +T data
    }

    PostController ..> CreatePostRequestDTO : receives
    PostController ..> ApiResponseDTO : returns
    ApiResponseDTO ..> PostResponseDTO : wraps
```

---

## 3. DTO Class Specification & Diagram

`ApiResponseDTO<T>` และ `FieldErrorDTO` ใช้ร่วมกันทุก task ดูที่ [API Conventions](../common/API-CONVENTIONS.md)

1. **`CreatePostRequestDTO`**
   - `productDetail`: String (Not Blank, Max 500)
   - `shopName`: String (Not Blank, Max 100)
   - `location`: String (Not Blank, Max 255)
   - `receivingDetail`: String (Not Blank, Max 500)
2. **`PostResponseDTO`**
   - `postId`: Long
   - `productDetail`, `shopName`, `location`, `receivingDetail`: String
   - `status`: String (`OPEN` เมื่อสร้างใหม่, `ACCEPTED` เมื่อมีคนรับ)
   - `buyerId`: Long (เจ้าของโพสต์ จาก JWT)
   - `buyerName`: String (จาก `USERS.full_name`)
   - `acceptedBy`: Long (ว่างจนกว่าจะมีคนรับ)
   - `acceptedByName`: String (ว่างจนกว่าจะมีคนรับ)
   - `acceptedAt`: LocalDateTime (ว่างจนกว่าจะมีคนรับ)
   - `createdAt`: LocalDateTime

```mermaid
classDiagram
    class CreatePostRequestDTO {
        -String productDetail
        -String shopName
        -String location
        -String receivingDetail
        +getProductDetail() String
        +getShopName() String
        +getLocation() String
        +getReceivingDetail() String
    }
    class PostResponseDTO {
        -Long postId
        -String productDetail
        -String shopName
        -String location
        -String receivingDetail
        -String status
        -Long buyerId
        -String buyerName
        -Long acceptedBy
        -String acceptedByName
        -LocalDateTime acceptedAt
        -LocalDateTime createdAt
        +getPostId() Long
        +getProductDetail() String
        +getShopName() String
        +getLocation() String
        +getReceivingDetail() String
        +getStatus() String
        +getBuyerId() Long
        +getBuyerName() String
        +getAcceptedBy() Long
        +getAcceptedByName() String
        +getAcceptedAt() LocalDateTime
        +getCreatedAt() LocalDateTime
    }
```

---

## 4. DAO / Repository Specification & Diagram

### Entity `Post` (ตาราง `posts`)

| Field | Column | หมายเหตุ |
|---|---|---|
| `id` | `id` | PK, auto increment |
| `buyer` (`User`) | `buyer_id` | FK → `USERS.id` (`@ManyToOne`), NOT NULL |
| `productDetail` | `product_detail` | NOT NULL |
| `shopName` | `shop_name` | NOT NULL |
| `location` | `location` | NOT NULL |
| `receivingDetail` | `receiving_detail` | NOT NULL |
| `status` | `status` | NOT NULL, DEFAULT `OPEN` (`OPEN`, `ACCEPTED`) |
| `createdAt` | `created_at` | NOT NULL, DEFAULT CURRENT_TIMESTAMP |
| `acceptedBy` (`User`) | `accepted_by` | FK → `USERS.id` (`@ManyToOne`), ว่างได้ |
| `acceptedAt` | `accepted_at` | ว่างได้ |

ความสัมพันธ์: `USERS` 1 ──< `POSTS` (เจ้าของโพสต์) และ `USERS` 0..1 ──< `POSTS` (ผู้รับงาน) ตรงกับ ERD กลาง

### Repository
- **Interface:** `PostRepository` (extends `JpaRepository<Post, Long>`)
- **Methods:**
  - `Post save(Post post)` - สร้างโพสต์ (Endpoint 1) และบันทึกการรับงาน (Endpoint 5)
  - `List<Post> findByStatusOrderByCreatedAtDesc(String status)` - โพสต์ `OPEN` (Endpoint 2)
  - `List<Post> findByBuyerIdOrderByCreatedAtDesc(Long buyerId)` - โพสต์ของฉัน (Endpoint 3)
  - `Optional<Post> findById(Long id)` - รายละเอียด/รับงาน (Endpoint 4, 5)
- **ใช้ร่วมกับ Task04:** `UserRepository.findById` ดึงผู้ใช้จาก JWT

```mermaid
classDiagram
    class PostRepository {
        <<interface>>
        +save(Post post) Post
        +findById(Long id) Optional~Post~
        +findByStatusOrderByCreatedAtDesc(String status) List~Post~
        +findByBuyerIdOrderByCreatedAtDesc(Long buyerId) List~Post~
    }
    class Post {
        -Long id
        -User buyer
        -String productDetail
        -String shopName
        -String location
        -String receivingDetail
        -String status
        -LocalDateTime createdAt
        -User acceptedBy
        -LocalDateTime acceptedAt
    }
    class User {
        -Long id
        -String fullName
    }
    PostRepository ..> Post : manages
    Post --> User : buyer
    Post --> User : acceptedBy
```

---

## 5. Extended API Class Diagram (Full Layered Architecture)

Controller เรียก Service, Service ใช้ `PostRepository` และ `UserRepository` (Task04) ผู้ใช้มาจาก JWT

```mermaid
classDiagram
    class PostController {
        -PostService postService
        +createPost(CreatePostRequestDTO request) ResponseEntity~ApiResponseDTO~
        +getOpenPosts() ResponseEntity~ApiResponseDTO~
        +getMyPosts() ResponseEntity~ApiResponseDTO~
        +getPost(Long postId) ResponseEntity~ApiResponseDTO~
        +acceptPost(Long postId) ResponseEntity~ApiResponseDTO~
    }
    class PostService {
        <<interface>>
        +createPost(Long userId, CreatePostRequestDTO request) PostResponseDTO
        +getOpenPosts() List~PostResponseDTO~
        +getMyPosts(Long userId) List~PostResponseDTO~
        +getPost(Long postId) PostResponseDTO
        +acceptPost(Long userId, Long postId) PostResponseDTO
    }
    class PostServiceImpl {
        -PostRepository postRepository
        -UserRepository userRepository
    }
    class PostRepository {
        <<interface>>
        +save(Post post) Post
        +findById(Long id) Optional~Post~
        +findByStatusOrderByCreatedAtDesc(String status) List~Post~
        +findByBuyerIdOrderByCreatedAtDesc(Long buyerId) List~Post~
    }
    class UserRepository {
        <<interface>>
        +findById(Long id) Optional~User~
    }
    class Post {
        -Long id
        -User buyer
        -String productDetail
        -String shopName
        -String location
        -String receivingDetail
        -String status
        -LocalDateTime createdAt
        -User acceptedBy
        -LocalDateTime acceptedAt
    }
    class User {
        -Long id
        -String fullName
    }
    class CreatePostRequestDTO
    class PostResponseDTO
    class ApiResponseDTO~T~ {
        -String status
        -String message
        -T data
        -List~FieldErrorDTO~ errors
    }

    PostController --> PostService : uses
    PostController ..> ApiResponseDTO : returns
    PostServiceImpl ..|> PostService : implements
    PostServiceImpl --> PostRepository : uses
    PostServiceImpl --> UserRepository : uses
    PostRepository ..> Post : manages
    UserRepository ..> User : manages
    Post --> User : buyer
    Post --> User : acceptedBy
    PostServiceImpl ..> CreatePostRequestDTO : accepts
    PostServiceImpl ..> PostResponseDTO : creates
    ApiResponseDTO ..> PostResponseDTO : wraps
```
