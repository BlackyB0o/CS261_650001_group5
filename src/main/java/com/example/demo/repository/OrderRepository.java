package com.example.demo.repository;

import com.example.demo.entity.Order;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /** ออเดอร์ของผู้ฝากหิ้ว ล่าสุดก่อน (GET /orders/my) */
    List<Order> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    /** ออเดอร์ทั้งหมดในรอบ เก่าไปใหม่ (GET /orders?roundId=) */
    List<Order> findByRoundIdOrderByCreatedAtAsc(Long roundId);
}
