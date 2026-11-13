package com.example.demo.repository;

import com.example.demo.entity.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /** ออเดอร์ของผู้ฝากหิ้ว ล่าสุดก่อน (GET /orders/my) */
    List<Order> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    /** ออเดอร์ทั้งหมดในรอบ เก่าไปใหม่ (GET /orders?roundId=) */
    List<Order> findByRoundIdOrderByCreatedAtAsc(Long roundId);

    /**
     * Task36 (US04) ออเดอร์ทั้งหมดในรอบ พร้อมผู้ฝากหิ้วและรายการสินค้า ใน query เดียว
     * เรียงเก่าไปใหม่ (เวลาเท่ากันเรียงตาม id) ใช้กับ GET /delivery-rounds/{roundId}/orders และ GET /orders?roundId=
     */
    @Query("""
            SELECT o FROM Order o
            JOIN FETCH o.buyer
            LEFT JOIN FETCH o.items
            WHERE o.round.id = :roundId
            ORDER BY o.createdAt ASC, o.id ASC
            """)
    List<Order> findByRoundIdWithBuyerAndItems(@Param("roundId") Long roundId);

    /** Task41 (US05) ออเดอร์หนึ่งรายการ พร้อมผู้ฝากหิ้ว รอบ ผู้รับหิ้ว และรายการสินค้า ใน query เดียว */
    @Query("""
            SELECT o FROM Order o
            JOIN FETCH o.buyer
            JOIN FETCH o.round r
            JOIN FETCH r.runner
            LEFT JOIN FETCH o.items
            WHERE o.id = :orderId
            """)
    Optional<Order> findByIdWithDetails(@Param("orderId") Long orderId);
}
