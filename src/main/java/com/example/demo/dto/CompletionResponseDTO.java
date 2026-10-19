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
public class CompletionResponseDTO {

    private Long completionId;
    private Long orderId;
    private Long paymentId;
    /** ผู้ฝากหิ้วที่กดยืนยัน */
    private Long confirmedBy;
    private LocalDateTime confirmedAt;
    private String completionStatus;
    private String evidenceImageUrl;
    /** ผู้รับหิ้วที่ได้รับเงิน */
    private Long runnerId;
    /** เท่ากับ payments.total_amount */
    private BigDecimal releasedAmount;
    private String orderStatus;
    private String paymentStatus;
}
