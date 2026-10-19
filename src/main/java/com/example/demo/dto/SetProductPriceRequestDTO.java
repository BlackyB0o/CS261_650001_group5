package com.example.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** PUT /api/v1/orders/{orderId}/price (ผู้รับหิ้วระบุราคา) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SetProductPriceRequestDTO {

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    private BigDecimal productAmount;
}
