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
public class DeliveryRoundResponseDTO {

    private Long roundId;
    private Long runnerId;
    private String runnerName;
    private String storeName;
    private String locationName;
    private LocalDateTime scheduledAt;
    private BigDecimal serviceFee;
    private String status;
}
