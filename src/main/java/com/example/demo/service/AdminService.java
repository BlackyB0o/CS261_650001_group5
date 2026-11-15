package com.example.demo.service;

import com.example.demo.dto.AdminReportResponseDTO;
import com.example.demo.dto.ReportDecisionRequestDTO;
import com.example.demo.dto.UserSuspensionResponseDTO;
import java.util.List;

/**
 * Task56 (US10) งานของผู้ดูแลระบบ ทุก method ตรวจก่อนว่าผู้เรียกเป็นแอดมิน
 * ไม่ใช่แอดมิน 403 "Admin access required"
 */
public interface AdminService {

    /** รายงานทั้งหมด ใหม่สุดก่อน */
    List<AdminReportResponseDTO> getReports(Long adminId);

    /** 404 ไม่พบรายงาน */
    AdminReportResponseDTO getReport(Long adminId, Long reportId);

    /** อนุมัติหรือปฏิเสธรายงาน 404 ไม่พบรายงาน, 409 พิจารณาไปแล้ว */
    AdminReportResponseDTO reviewReport(Long adminId, Long reportId, ReportDecisionRequestDTO request);

    /** ระงับ (true) หรือคืนสิทธิ์ (false) 404 ไม่พบผู้ใช้, 403 บัญชีเป้าหมายเป็นแอดมิน */
    UserSuspensionResponseDTO setUserSuspended(Long adminId, Long targetUserId, boolean suspended);
}
