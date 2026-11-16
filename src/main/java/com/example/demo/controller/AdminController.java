package com.example.demo.controller;

import com.example.demo.common.RequestAttributes;
import com.example.demo.dto.AdminReportResponseDTO;
import com.example.demo.dto.ApiResponseDTO;
import com.example.demo.dto.ReportDecisionRequestDTO;
import com.example.demo.dto.SuspendUserRequestDTO;
import com.example.demo.dto.UserSuspensionResponseDTO;
import com.example.demo.service.AdminService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Task56 (US10) endpoint ของผู้ดูแลระบบ ต้องมี token ของแอดมิน (ผู้ใช้ทั่วไปได้ 403) */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/reports")
    public ResponseEntity<ApiResponseDTO<List<AdminReportResponseDTO>>> getReports(
            @RequestAttribute(RequestAttributes.USER_ID) Long userId) {
        return ResponseEntity.ok(ApiResponseDTO.success(adminService.getReports(userId)));
    }

    @GetMapping("/reports/{reportId}")
    public ResponseEntity<ApiResponseDTO<AdminReportResponseDTO>> getReport(
            @RequestAttribute(RequestAttributes.USER_ID) Long userId,
            @PathVariable Long reportId) {
        return ResponseEntity.ok(ApiResponseDTO.success(adminService.getReport(userId, reportId)));
    }

    /** body: { "decision": "APPROVED" | "DECLINED", "adminNote": "..." (ไม่บังคับ) } */
    @PatchMapping("/reports/{reportId}/decision")
    public ResponseEntity<ApiResponseDTO<AdminReportResponseDTO>> reviewReport(
            @RequestAttribute(RequestAttributes.USER_ID) Long userId,
            @PathVariable Long reportId,
            @Valid @RequestBody ReportDecisionRequestDTO request) {
        AdminReportResponseDTO data = adminService.reviewReport(userId, reportId, request);
        return ResponseEntity.ok(ApiResponseDTO.success("Report reviewed successfully", data));
    }

    /** body: { "suspended": true } ระงับบัญชี หรือ { "suspended": false } คืนสิทธิ์บัญชี */
    @PatchMapping("/users/{userId}/suspend")
    public ResponseEntity<ApiResponseDTO<UserSuspensionResponseDTO>> suspendUser(
            @RequestAttribute(RequestAttributes.USER_ID) Long adminId,
            @PathVariable("userId") Long targetUserId,
            @Valid @RequestBody SuspendUserRequestDTO request) {
        UserSuspensionResponseDTO data = adminService.setUserSuspended(adminId, targetUserId, request.getSuspended());
        String message = data.getSuspended() ? "User suspended successfully" : "User unsuspended successfully";
        return ResponseEntity.ok(ApiResponseDTO.success(message, data));
    }
}
