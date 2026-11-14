package com.example.demo.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** ผลของ POST /api/v1/reports (Task51 US09) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponseDTO {

    private Long reportId;
    private Long orderId;
    private Long reporterId;
    private Long reportedUserId;
    private String reportedUserName;
    private String category;
    private String description;
    private String evidenceUrl;
    private String status;
    private LocalDateTime createdAt;
}
