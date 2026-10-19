package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;
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
public class OrderResponseDTO {

    private Long orderId;
    private Long roundId;
    private Long buyerId;
    private String buyerName;
    private String handoffMethod;
    private String handoffLocation;
    /** PENDING เมื่อสร้างใหม่ */
    private String status;
    private LocalDateTime createdAt;
    private List<OrderItemResponseDTO> items;
}
