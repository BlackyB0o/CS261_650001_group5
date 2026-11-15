package com.example.demo.repository;

import com.example.demo.entity.Report;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Task51 (US09) บันทึกรายงาน, Task54 (US10) แอดมินดูและพิจารณารายงาน, ผู้ใช้ดูประวัติรายงานของตัวเอง */
public interface ReportRepository extends JpaRepository<Report, Long> {

    /** รายงานทั้งหมด ใหม่สุดก่อน พร้อมออเดอร์ ผู้รายงาน และผู้ถูกรายงาน (แอดมิน) */
    @EntityGraph(attributePaths = {"order", "reporter", "reportedUser"})
    List<Report> findAllByOrderByCreatedAtDescIdDesc();

    /** รายงานหนึ่งรายการ พร้อมออเดอร์ ผู้รายงาน และผู้ถูกรายงาน (แอดมิน) */
    @EntityGraph(attributePaths = {"order", "reporter", "reportedUser"})
    Optional<Report> findWithDetailsById(Long id);

    /** รายงานที่ผู้ใช้คนนี้ส่ง ใหม่สุดก่อน (ประวัติรายงานของตัวเอง) */
    @EntityGraph(attributePaths = {"order", "reportedUser"})
    List<Report> findByReporterIdOrderByCreatedAtDescIdDesc(Long reporterId);
}
