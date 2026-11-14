package com.example.demo.controller;

import com.example.demo.common.RequestAttributes;
import com.example.demo.dto.ApiResponseDTO;
import com.example.demo.dto.CreateReportRequestDTO;
import com.example.demo.dto.ReportResponseDTO;
import com.example.demo.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Task51 (US09) รายงานปัญหาหรือพฤติกรรมน่าสงสัย ต้องมี token (userId จาก JWT) */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** multipart/form-data: orderId, category, description, evidenceImage (ไม่บังคับ) */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseDTO<ReportResponseDTO>> createReport(
            @RequestAttribute(RequestAttributes.USER_ID) Long userId,
            @Valid @ModelAttribute CreateReportRequestDTO request) {
        ReportResponseDTO data = reportService.createReport(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success("Report submitted successfully", data));
    }
}
