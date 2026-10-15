# CS261 Group 5 - Database ER Diagram

## Database ER Diagram (Tasks 04, 08, 16, 20, 24)

```mermaid
erDiagram
    %% Task 08 - เปิดรอบรับหิ้ว
    USERS ||--o{ DELIVERY_ROUNDS : opens
    
    %% Task 16 - ความสัมพันธ์ของรายการสั่งซื้อ
    USERS ||--o{ ORDERS : places
    DELIVERY_ROUNDS ||--o{ ORDERS : receives
    ORDERS ||--|{ ORDER_ITEMS : contains
    
    %% Task 04 - ลงทะเบียนหรือเข้าสู่ระบบ
    USERS {
        bigint id PK
        varchar email UK "NOT NULL"
        varchar password_hash "NOT NULL"
        varchar full_name "NOT NULL"
        varchar phone_number UK "NOT NULL"
        varchar role "NOT NULL"
        timestamp created_at "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
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
```
