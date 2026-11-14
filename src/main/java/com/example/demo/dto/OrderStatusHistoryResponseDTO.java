package com.example.demo.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** ประวัติสถานะหนึ่งครั้งในรายละเอียดออเดอร์ (Task41 US05) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistoryResponseDTO {

    /** null เมื่อเป็นการสร้างออเดอร์ */
    private String fromStatus;
    private String toStatus;
    private Long changedById;
    private String changedByName;
    private LocalDateTime changedAt;
}
