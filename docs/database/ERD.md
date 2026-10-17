# CS261 Group 5 - Database ER Diagram

## Database ER Diagram (Tasks 04, 08, 12, 16, 20, 24)

```mermaid
erDiagram
    %% Task 08 - เปิดรอบรับหิ้ว
    USERS ||--o{ DELIVERY_ROUNDS : opens
    
    %% Task 16 - ความสัมพันธ์ของรายการสั่งซื้อ
    USERS ||--o{ ORDERS : places
    DELIVERY_ROUNDS ||--o{ ORDERS : receives
    USERS ||--o{ POSTS : creates
    USERS |o--o{ POSTS : accepts
    ORDERS ||--|{ ORDER_ITEMS : contains

    %% Task 20, 24 - ความสัมพันธ์การชำระเงินและยืนยันรับสินค้า
    ORDERS ||--o| PAYMENTS : has
    PAYMENTS ||--o| ORDER_COMPLETIONS : leads_to
    USERS ||--o{ ORDER_COMPLETIONS : confirms
    
    %% Task 04 - ลงทะเบียนหรือเข้าสู่ระบบ
    USERS {
        bigint id PK
        varchar email UK "NOT NULL"
        varchar password_hash "NOT NULL"
        varchar full_name "NOT NULL"
        varchar phone_number UK "NOT NULL"
        varchar role "NOT NULL, DEFAULT USER"
        timestamp created_at "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
    }

    %% Task 12 - ผู้ฝากหิ้วโพสต์หาคนรับหิ้ว
    POSTS {
        bigint id PK
        bigint buyer_id FK "NOT NULL"
        varchar product_detail "NOT NULL"
        varchar shop_name "NOT NULL"
        varchar location "NOT NULL"
        varchar receiving_detail "NOT NULL"
        varchar status "NOT NULL, DEFAULT OPEN"
        timestamp created_at "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
        bigint accepted_by FK "OPTIONAL"
        timestamp accepted_at "OPTIONAL"
    }
    %% Task 08 - เปิดรอบรับหิ้ว
    DELIVERY_ROUNDS {
        bigint id PK
        bigint runner_id FK "NOT NULL"
        varchar store_name
        varchar location_name
        timestamp scheduled_at "NOT NULL"
        decimal service_fee "NOT NULL, >=0"
        varchar status "NOT NULL"
    }

    %% Task 16 - ฝากซื้อสินค้า/อาหาร
    ORDERS {
        bigint id PK
        bigint buyer_id FK "NOT NULL"
        bigint round_id FK "NOT NULL"
        varchar handoff_method "NOT NULL"
        varchar handoff_location "NOT NULL"
        varchar status "NOT NULL"
        timestamp created_at "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
    }

    %% Task 16 - รายการสินค้าในคำสั่งซื้อ
    ORDER_ITEMS {
        bigint id PK
        bigint order_id FK "NOT NULL"
        varchar product_name "NOT NULL"
        int quantity "NOT NULL, > 0"
        varchar note "OPTIONAL"
    }

    %% Task 20 - ชำระค่าสินค้าและค่าหิ้ว
    PAYMENTS {
        bigint id PK
        bigint order_id FK,UK "NOT NULL"
        decimal product_amount "NOT NULL, >=0"
        decimal service_fee "NOT NULL, >=0"
        decimal total_amount "NOT NULL, >=0"
        varchar payment_method "NOT NULL"
        varchar qr_payload
        varchar slip_image_url
        varchar payment_status "NOT NULL"
        varchar transaction_reference
        timestamp paid_at
        timestamp created_at "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
    }

    %% Task 24 - ยืนยันรับสินค้าและปิดออเดอร์
    ORDER_COMPLETIONS {
        bigint id PK
        bigint payment_id FK,UK "NOT NULL"
        bigint confirmed_by FK "NOT NULL"
        varchar evidence_image_url "NOT NULL"
        timestamp confirmed_at "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
        varchar completion_status "NOT NULL"
    }
```
