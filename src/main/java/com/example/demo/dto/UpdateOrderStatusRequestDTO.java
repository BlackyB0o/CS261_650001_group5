package com.example.demo.dto;

import com.example.demo.common.OrderStatusRules;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** PATCH /api/v1/orders/{orderId}/status (Task46 US06) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequestDTO {

    @NotBlank
    @Pattern(regexp = OrderStatusRules.ALL_STATUSES,
            message = "must be one of PENDING, PAID, ACCEPTED, PURCHASING, READY_FOR_DELIVERY, DELIVERING, COMPLETED, CANCELLED")
    private String status;
}
