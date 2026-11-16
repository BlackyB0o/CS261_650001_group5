package com.example.demo.service.impl;

import com.example.demo.common.ReportStatuses;
import com.example.demo.common.UserRoles;
import com.example.demo.dto.AdminReportResponseDTO;
import com.example.demo.dto.ReportDecisionRequestDTO;
import com.example.demo.dto.UserSuspensionResponseDTO;
import com.example.demo.entity.Report;
import com.example.demo.entity.User;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.ReportRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AdminService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Task56 (US10) แอดมินดูและพิจารณารายงาน และระงับหรือคืนสิทธิ์บัญชีผู้ใช้ */
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AdminReportResponseDTO> getReports(Long adminId) {
        requireAdmin(adminId);
        return reportRepository.findAllByOrderByCreatedAtDescIdDesc().stream()
                .map(AdminServiceImpl::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminReportResponseDTO getReport(Long adminId, Long reportId) {
        requireAdmin(adminId);
        return toResponse(findReport(reportId));
    }

    @Override
    @Transactional
    public AdminReportResponseDTO reviewReport(Long adminId, Long reportId, ReportDecisionRequestDTO request) {
        requireAdmin(adminId);
        Report report = findReport(reportId);
        if (!ReportStatuses.PENDING.equals(report.getStatus())) {
            throw ApiException.conflict("Report has already been reviewed");
        }
        String note = request.getAdminNote() == null ? null : request.getAdminNote().trim();
        report.setStatus(request.getDecision());
        report.setAdminNote(note == null || note.isEmpty() ? null : note);
        report.setReviewedAt(LocalDateTime.now());
        return toResponse(reportRepository.save(report));
    }

    @Override
    @Transactional
    public UserSuspensionResponseDTO setUserSuspended(Long adminId, Long targetUserId, boolean suspended) {
        requireAdmin(adminId);
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        if (UserRoles.ADMIN.equals(target.getRole())) {
            throw ApiException.forbidden("Admin accounts cannot be suspended");
        }
        if (target.isSuspended() != suspended) {
            target.setSuspended(suspended);
            target = userRepository.save(target);
        }
        return UserSuspensionResponseDTO.builder()
                .userId(target.getId())
                .fullName(target.getFullName())
                .suspended(target.isSuspended())
                .build();
    }

    /** ผู้เรียกต้องเป็นแอดมิน (ตรวจ role จากฐานข้อมูล ไม่เชื่อค่าจาก request) */
    private void requireAdmin(Long adminId) {
        boolean admin = userRepository.findById(adminId)
                .map(user -> UserRoles.ADMIN.equals(user.getRole()))
                .orElse(false);
        if (!admin) {
            throw ApiException.forbidden("Admin access required");
        }
    }

    private Report findReport(Long reportId) {
        return reportRepository.findWithDetailsById(reportId)
                .orElseThrow(() -> ApiException.notFound("Report not found"));
    }

    private static AdminReportResponseDTO toResponse(Report report) {
        User reporter = report.getReporter();
        User reported = report.getReportedUser();
        boolean reporterIsBuyer = report.getOrder().getBuyer().getId().equals(reporter.getId());
        return AdminReportResponseDTO.builder()
                .reportId(report.getId())
                .orderId(report.getOrder().getId())
                .reporterId(reporter.getId())
                .reporterName(reporter.getFullName())
                .reporterType(reporterIsBuyer ? "BUYER" : "RUNNER")
                .reportedUserId(reported.getId())
                .reportedUserName(reported.getFullName())
                .reportedUserRole(reported.getRole())
                .reportedUserSuspended(reported.isSuspended())
                .category(report.getCategory())
                .description(report.getDescription())
                .evidenceUrl(report.getEvidenceUrl())
                .status(report.getStatus())
                .adminNote(report.getAdminNote())
                .createdAt(report.getCreatedAt())
                .reviewedAt(report.getReviewedAt())
                .build();
    }
}
