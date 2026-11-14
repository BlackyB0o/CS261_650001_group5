package com.example.demo.service.impl;

import com.example.demo.dto.CreateReportRequestDTO;
import com.example.demo.dto.ReportResponseDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.Report;
import com.example.demo.entity.User;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ReportRepository;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Task51 (US09) ผู้รายงานต้องเป็นคู่กรณีในออเดอร์ ระบบระบุผู้ถูกรายงานให้เองเป็นอีกฝ่าย */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final OrderRepository orderRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public ReportResponseDTO createReport(Long reporterId, CreateReportRequestDTO request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        User buyer = order.getBuyer();
        User runner = order.getRound().getRunner();
        User reporter;
        User reportedUser;
        if (buyer.getId().equals(reporterId)) {
            reporter = buyer;
            reportedUser = runner;
        } else if (runner.getId().equals(reporterId)) {
            reporter = runner;
            reportedUser = buyer;
        } else {
            throw ApiException.forbidden("You are not involved in this order");
        }

        Report report = new Report();
        report.setOrder(order);
        report.setReporter(reporter);
        report.setReportedUser(reportedUser);
        report.setCategory(request.getCategory());
        report.setDescription(request.getDescription().trim());
        if (request.getEvidenceImage() != null && !request.getEvidenceImage().isEmpty()) {
            report.setEvidenceUrl(fileStorageService.store(request.getEvidenceImage(), "reports"));
        }
        report.setStatus("PENDING");
        Report saved = reportRepository.save(report);

        return ReportResponseDTO.builder()
                .reportId(saved.getId())
                .orderId(order.getId())
                .reporterId(reporter.getId())
                .reportedUserId(reportedUser.getId())
                .reportedUserName(reportedUser.getFullName())
                .category(saved.getCategory())
                .description(saved.getDescription())
                .evidenceUrl(saved.getEvidenceUrl())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
