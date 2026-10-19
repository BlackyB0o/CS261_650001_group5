package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponseDTO {

    private Long postId;
    private String postTitle;
    private String shopName;
    private String productDetail;
    private Integer quantity;
    private BigDecimal carryFee;
    private LocalDate desiredDate;
    private String location;
    private BigDecimal budget;
    private String additionalDetails;
    /** OPEN เมื่อสร้างใหม่ ACCEPTED เมื่อมีคนรับงาน */
    private String status;
    private Long buyerId;
    private String buyerName;
    private Long acceptedBy;
    private String acceptedByName;
    private LocalDateTime acceptedAt;
    private LocalDateTime createdAt;
}
