package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ตาราง reports (Task51 US09) รายงานปัญหาหรือพฤติกรรมน่าสงสัยของคู่กรณีในออเดอร์
 * ผู้รายงานเป็นผู้ฝากหิ้วหรือผู้รับหิ้วของออเดอร์ ส่วนผู้ถูกรายงานคืออีกฝ่าย
 */
@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_user_id", nullable = false)
    private User reportedUser;

    /** PRODUCT_MISMATCH, DELIVERY_ISSUE, PAYMENT_ISSUE, SUSPICIOUS_BEHAVIOR, OTHER */
    @Column(nullable = false, length = 30)
    private String category;

    @Column(nullable = false, length = 1000)
    private String description;

    /** path รูปหลักฐาน null ได้ (ไม่แนบรูป) */
    @Column(name = "evidence_url", length = 500)
    private String evidenceUrl;

    /** รายงานใหม่เป็น PENDING เสมอ */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (status == null) {
            status = "PENDING";
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
