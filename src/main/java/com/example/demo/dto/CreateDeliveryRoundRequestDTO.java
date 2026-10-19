package com.example.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** POST /api/v1/delivery-rounds (runnerId มาจาก JWT) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateDeliveryRoundRequestDTO {

    @NotBlank
    private String storeName;

    @NotBlank
    private String locationName;

    @NotNull
    @Future
    private LocalDateTime scheduledAt;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal serviceFee;
}
