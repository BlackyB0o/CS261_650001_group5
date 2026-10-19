package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** ตาราง delivery_rounds (Task08) status: OPEN, CLOSED, COMPLETED, CANCELLED */
@Entity
@Table(name = "delivery_rounds")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** เจ้าของรอบ = ผู้รับหิ้วของรอบนี้ */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "runner_id", nullable = false)
    private User runner;

    @Column(name = "store_name", nullable = false)
    private String storeName;

    @Column(name = "location_name", nullable = false)
    private String locationName;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "service_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal serviceFee;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "OPEN";
}
