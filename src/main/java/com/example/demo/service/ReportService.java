package com.example.demo.service;

import com.example.demo.dto.CreateReportRequestDTO;
import com.example.demo.dto.ReportResponseDTO;

/** Task51 (US09) รายงานปัญหาหรือพฤติกรรมน่าสงสัย */
public interface ReportService {

    /**
     * ผู้ฝากหิ้วหรือผู้รับหิ้วของออเดอร์ รายงานอีกฝ่าย
     * 404 ไม่พบออเดอร์, 403 ผู้รายงานไม่ใช่คู่กรณีในออเดอร์
     */
    ReportResponseDTO createReport(Long reporterId, CreateReportRequestDTO request);
}
