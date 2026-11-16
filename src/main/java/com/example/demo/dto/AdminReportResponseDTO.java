package com.example.demo.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Task54 (US10) รายงานในมุมมองของแอดมิน
 * ใช้กับ GET /api/v1/admin/reports, GET /api/v1/admin/reports/{reportId} และ PATCH .../decision
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminReportResponseDTO {

    private Long reportId;
    private Long orderId;
    private Long reporterId;
    private String reporterName;
    /** BUYER = ผู้ฝากหิ้วเป็นคนรายงาน, RUNNER = ผู้รับหิ้วเป็นคนรายงาน */
    private String reporterType;
    private Long reportedUserId;
    private String reportedUserName;
    /** USER หรือ ADMIN */
    private String reportedUserRole;
    /** บัญชีผู้ถูกรายงานถูกระงับอยู่หรือไม่ */
    private Boolean reportedUserSuspended;
    private String category;
    private String description;
    private String evidenceUrl;
    /** PENDING, APPROVED หรือ DECLINED */
    private String status;
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
