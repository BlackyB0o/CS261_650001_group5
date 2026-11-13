package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** ผลของ PATCH /api/v1/orders/{orderId}/status (Task46 US06) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusResponseDTO {

    private Long orderId;
    private String status;
}
