package com.example.demo.dto;

import java.math.BigDecimal;
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
public class PaymentResponseDTO {

    private Long paymentId;
    private Long orderId;
    private BigDecimal productAmount;
    private BigDecimal serviceFee;
    private BigDecimal totalAmount;
    /** QR_TRANSFER */
    private String paymentMethod;
    /** PENDING หรือ HELD */
    private String paymentStatus;
    /** ข้อมูลสำหรับสร้าง QR ของระบบตัวกลาง */
    private String qrPayload;
    private String slipImageUrl;
    private String transactionReference;
    private LocalDateTime paidAt;
    private String orderStatus;
}
