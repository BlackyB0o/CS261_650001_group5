package com.example.demo.repository;

import com.example.demo.entity.DeliveryRound;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRoundRepository extends JpaRepository<DeliveryRound, Long> {

    /** รอบตามสถานะ เรียงใกล้ไปไกล (GET /delivery-rounds) */
    List<DeliveryRound> findByStatusOrderByScheduledAtAsc(String status);

    /** ประวัติรอบของผู้รับหิ้ว เรียงล่าสุดก่อน (GET /delivery-rounds/my) */
    List<DeliveryRound> findByRunnerIdOrderByScheduledAtDesc(Long runnerId);
}
