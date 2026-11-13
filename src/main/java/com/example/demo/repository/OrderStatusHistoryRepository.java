package com.example.demo.repository;

import com.example.demo.entity.OrderStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Task46 (US06) */
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    /** ประวัติสถานะของออเดอร์ เรียงเก่าไปใหม่ (เวลาเท่ากันเรียงตาม id) ดึงผู้เปลี่ยนสถานะมาด้วย (Task41 US05) */
    @EntityGraph(attributePaths = "changedBy")
    List<OrderStatusHistory> findByOrderIdOrderByChangedAtAscIdAsc(Long orderId);
}
