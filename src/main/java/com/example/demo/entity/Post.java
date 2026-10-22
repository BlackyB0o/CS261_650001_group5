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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** ตาราง posts (Task12) โพสต์หาคนรับหิ้ว status: OPEN, ACCEPTED (Sprint 1) */
@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** เจ้าของโพสต์ = ผู้ฝากหิ้ว */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @Column(name = "post_title", nullable = false)
    private String postTitle;

    @Column(name = "shop_name", nullable = false)
    private String shopName;

    @Column(name = "product_detail", nullable = false, columnDefinition = "TEXT")
    private String productDetail;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "carry_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal carryFee;

    @Column(name = "desired_date", nullable = false)
    private LocalDate desiredDate;

    @Column(name = "location", nullable = false)
    private String location;

    @Column(name = "budget", precision = 10, scale = 2)
    private BigDecimal budget;

    @Column(name = "additional_details", columnDefinition = "TEXT")
    private String additionalDetails;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "OPEN";

    /** ผู้รับงาน (ว่างจนกว่าจะมีคนกดรับ) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by")
    private User acceptedBy;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
