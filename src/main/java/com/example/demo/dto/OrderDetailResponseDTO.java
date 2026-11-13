package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GET /api/v1/orders/{orderId} (Task41 US05)
 * field เดิมเหมือน OrderResponseDTO ทุกตัว และเพิ่มผู้รับหิ้วกับประวัติสถานะ
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailResponseDTO {

    private Long orderId;
    private Long roundId;
    private Long buyerId;
    private String buyerName;
    private Long runnerId;
    private String runnerName;
    private String handoffMethod;
    private String handoffLocation;
    private String status;
    private LocalDateTime createdAt;
    private List<OrderItemResponseDTO> items;
    /** เรียงเก่าไปใหม่ ออเดอร์ที่สร้างก่อน US06 อาจเป็นลิสต์ว่าง */
    private List<OrderStatusHistoryResponseDTO> statusHistory;
}
