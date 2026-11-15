package com.example.demo.dto;

import com.example.demo.common.ReportStatuses;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Task54 (US10) PATCH /api/v1/admin/reports/{reportId}/decision */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportDecisionRequestDTO {

    /** APPROVED (อนุมัติรายงาน) หรือ DECLINED (ปฏิเสธรายงาน) */
    @NotBlank
    @Pattern(regexp = ReportStatuses.DECISIONS, message = "must be APPROVED or DECLINED")
    private String decision;

    /** หมายเหตุจากแอดมิน ไม่บังคับ */
    @Size(max = 1000)
    private String adminNote;
}
