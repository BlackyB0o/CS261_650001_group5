# CS261 Group 5 - Database ER Diagram

## Database ER Diagram (Tasks 04, 08, 16, 20, 24)

```mermaid
erDiagram
    %% Task 08, 29 - เปิดรอบ / ค้นหารอบรับหิ้ว
    USERS ||--o{ DELIVERY_ROUNDS : opens

    %% Task 16, 34 - ฝากซื้อ / รายการฝากซื้อในรอบ
    USERS ||--o{ ORDERS : places
    DELIVERY_ROUNDS ||--o{ ORDERS : receives
    ORDERS ||--|{ ORDER_ITEMS : contains

    %% Task 20, 24 - ชำระเงิน / ยืนยันรับสินค้า
    ORDERS ||--o| PAYMENTS : has
    PAYMENTS ||--o| ORDER_COMPLETIONS : leads_to
    USERS ||--o{ ORDER_COMPLETIONS : confirms

    %% Task 04 - ลงทะเบียน/เข้าสู่ระบบ + OTP
    USERS ||--o{ OTP_VERIFICATIONS : verifies

    %% Task 12 - โพสต์หาคนรับหิ้ว
    USERS ||--o{ POSTS : creates
    USERS |o--o{ POSTS : accepts

    %% Task 04, 54 - ผู้ใช้ (role, suspended เพิ่มสำหรับ Admin US10)
    USERS {
        bigint id PK
        varchar email UK "NOT NULL"
        varchar password_hash "NOT NULL"
        varchar full_name "NOT NULL"
        varchar phone_number UK "OPTIONAL, 10 digits"
        varchar role "NOT NULL, DEFAULT USER (USER|ADMIN)"
        boolean suspended "NOT NULL, DEFAULT false"
        timestamp created_at "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
    }

    %% Task 08, 29 - รอบรับหิ้ว
    DELIVERY_ROUNDS {
        bigint id PK
        bigint runner_id FK "NOT NULL"
        varchar store_name "NOT NULL"
        varchar location_name "NOT NULL"
        timestamp scheduled_at "NOT NULL"
        decimal service_fee "NOT NULL, >=0"
        varchar status "NOT NULL, DEFAULT OPEN (OPEN|CLOSED|COMPLETED|CANCELLED)"
    }

    %% Task 16, 44 - ฝากซื้อสินค้า/อาหาร
    ORDERS {
        bigint id PK
        bigint buyer_id FK "NOT NULL"
        bigint round_id FK "NOT NULL"
        varchar handoff_method "NOT NULL (MEETUP|DELIVERY)"
        varchar handoff_location "NOT NULL"
        varchar status "NOT NULL, DEFAULT PENDING"
        timestamp created_at "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
    }

    %% Task 16 - รายการสินค้าในคำสั่งซื้อ
    ORDER_ITEMS {
        bigint id PK
        bigint order_id FK "NOT NULL"
        varchar product_name "NOT NULL"
        int quantity "NOT NULL, > 0"
        text note "OPTIONAL"
    }

    %% Task 20 - ชำระค่าสินค้าและค่าหิ้ว
    PAYMENTS {
        bigint id PK
        bigint order_id FK,UK "NOT NULL"
        decimal product_amount "NOT NULL, >=0"
        decimal service_fee "NOT NULL, >=0"
        decimal total_amount "NOT NULL, >=0"
        varchar payment_method "NOT NULL, DEFAULT QR_TRANSFER"
        varchar qr_payload "OPTIONAL"
        varchar slip_image_url "OPTIONAL"
        varchar payment_status "NOT NULL, DEFAULT PENDING (PENDING|HELD|RELEASED)"
        varchar transaction_reference "OPTIONAL"
        timestamp paid_at "OPTIONAL"
        timestamp created_at "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
    }

    %% Task 24 - ยืนยันรับสินค้าและปิดออเดอร์
    ORDER_COMPLETIONS {
        bigint id PK
        bigint payment_id FK,UK "NOT NULL"
        bigint confirmed_by FK "NOT NULL"
        varchar evidence_image_url "NOT NULL"
        timestamp confirmed_at "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
        varchar completion_status "NOT NULL, DEFAULT COMPLETED"
    }

    %% Task 04 - OTP Verification
    OTP_VERIFICATIONS {
        bigint id PK
        bigint user_id FK "NOT NULL"
        varchar otp_hash "NOT NULL"
        varchar purpose "NOT NULL (LOGIN|RESET_PASSWORD)"
        timestamp expires_at "NOT NULL"
        timestamp verified_at "OPTIONAL"
        int attempts_count "NOT NULL, DEFAULT 0"
        timestamp created_at "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
    }

    %% Task 12 - ผู้ฝากหิ้วโพสต์หาคนรับหิ้ว
    POSTS {
        bigint id PK
        bigint buyer_id FK "NOT NULL"
        varchar post_title "NOT NULL"
        varchar shop_name "NOT NULL"
        text product_detail "NOT NULL"
        int quantity "NOT NULL"
        decimal carry_fee "NOT NULL"
        date desired_date "NOT NULL"
        varchar location "NOT NULL"
        decimal budget "OPTIONAL"
        text additional_details "OPTIONAL"
        varchar status "NOT NULL, DEFAULT OPEN (OPEN|ACCEPTED)"
        bigint accepted_by FK "OPTIONAL"
        timestamp accepted_at "OPTIONAL"
        timestamp created_at "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
    }
```
