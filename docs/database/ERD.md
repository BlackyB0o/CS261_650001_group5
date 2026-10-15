# CS261 Group 5 - Database ER Diagram

## Task04 - User Registration and Login

```mermaid
erDiagram
    USERS {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar full_name
        varchar phone_number UK
        varchar role
        timestamp created_at
    }
```
