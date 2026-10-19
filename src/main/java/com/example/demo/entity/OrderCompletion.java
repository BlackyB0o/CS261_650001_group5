package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** ตาราง order_completions (Task24) หนึ่งการชำระเงินปิดได้ครั้งเดียว */
@Entity
@Table(name = "order_completions")
@Getter
@Setter
@NoArgsConstructor
public class OrderCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "confirmed_by", nullable = false)
    private User confirmedBy;

    @Column(name = "evidence_image_url", nullable = false, length = 500)
    private String evidenceImageUrl;

    @Column(name = "confirmed_at", nullable = false, updatable = false)
    private LocalDateTime confirmedAt;

    @Column(name = "completion_status", nullable = false, length = 20)
    private String completionStatus = "COMPLETED";

    @PrePersist
    void onCreate() {
        if (confirmedAt == null) {
            confirmedAt = LocalDateTime.now();
        }
    }
}
