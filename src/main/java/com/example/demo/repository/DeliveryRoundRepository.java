package com.example.demo.repository;

import com.example.demo.entity.DeliveryRound;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryRoundRepository extends JpaRepository<DeliveryRound, Long> {

    /** รอบตามสถานะ เรียงใกล้ไปไกล (GET /delivery-rounds) */
    List<DeliveryRound> findByStatusOrderByScheduledAtAsc(String status);

    /**
     * Task31 (US02) ค้นหารอบตามสถานะและคำค้น เรียงใกล้ไปไกล แบ่งหน้าตาม pageable
     * keyword ต้องเป็น pattern ตัวพิมพ์เล็กที่ครอบด้วย % แล้ว (ใช้ ! เป็นตัว escape) ไม่มีคำค้นให้ส่ง "%"
     */
    @Query(value = """
            SELECT r FROM DeliveryRound r
            WHERE r.status = :status
              AND (LOWER(r.storeName) LIKE :keyword ESCAPE '!'
                   OR LOWER(r.locationName) LIKE :keyword ESCAPE '!'
                   OR LOWER(r.runner.fullName) LIKE :keyword ESCAPE '!')
            ORDER BY r.scheduledAt ASC, r.id ASC
            """,
            countQuery = """
            SELECT COUNT(r) FROM DeliveryRound r
            WHERE r.status = :status
              AND (LOWER(r.storeName) LIKE :keyword ESCAPE '!'
                   OR LOWER(r.locationName) LIKE :keyword ESCAPE '!'
                   OR LOWER(r.runner.fullName) LIKE :keyword ESCAPE '!')
            """)
    Page<DeliveryRound> search(@Param("status") String status, @Param("keyword") String keyword,
            Pageable pageable);

    /** Task31 (US02) เหมือน search แต่จำกัดช่วงเวลา from (รวม) ถึง to (ไม่รวม) */
    @Query(value = """
            SELECT r FROM DeliveryRound r
            WHERE r.status = :status
              AND (LOWER(r.storeName) LIKE :keyword ESCAPE '!'
                   OR LOWER(r.locationName) LIKE :keyword ESCAPE '!'
                   OR LOWER(r.runner.fullName) LIKE :keyword ESCAPE '!')
              AND r.scheduledAt >= :from AND r.scheduledAt < :to
            ORDER BY r.scheduledAt ASC, r.id ASC
            """,
            countQuery = """
            SELECT COUNT(r) FROM DeliveryRound r
            WHERE r.status = :status
              AND (LOWER(r.storeName) LIKE :keyword ESCAPE '!'
                   OR LOWER(r.locationName) LIKE :keyword ESCAPE '!'
                   OR LOWER(r.runner.fullName) LIKE :keyword ESCAPE '!')
              AND r.scheduledAt >= :from AND r.scheduledAt < :to
            """)
    Page<DeliveryRound> searchByDate(@Param("status") String status, @Param("keyword") String keyword,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    /** ประวัติรอบของผู้รับหิ้ว เรียงล่าสุดก่อน (GET /delivery-rounds/my) */
    List<DeliveryRound> findByRunnerIdOrderByScheduledAtDesc(Long runnerId);
}
