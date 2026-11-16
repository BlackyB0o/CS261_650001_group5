package com.example.demo.service;

import com.example.demo.dto.CreateReportRequestDTO;
import com.example.demo.dto.ReportResponseDTO;
import java.util.List;

/** Task51 (US09) รายงานปัญหาหรือพฤติกรรมน่าสงสัย, Task56 (US10) ประวัติรายงานของตัวเอง */
public interface ReportService {

    /**
     * ผู้ฝากหิ้วหรือผู้รับหิ้วของออเดอร์ รายงานอีกฝ่าย
     * 404 ไม่พบออเดอร์, 403 ผู้รายงานไม่ใช่คู่กรณีในออเดอร์
     */
    ReportResponseDTO createReport(Long reporterId, CreateReportRequestDTO request);

    /** รายงานที่ผู้ใช้คนนี้ส่ง ใหม่สุดก่อน (status บอกว่าแอดมินพิจารณาแล้วหรือยัง) */
    List<ReportResponseDTO> getMyReports(Long reporterId);
}
