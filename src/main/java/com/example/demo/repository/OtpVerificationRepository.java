package com.example.demo.repository;

import com.example.demo.entity.OtpVerification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    /** OTP ล่าสุดของผู้ใช้ ใช้ตรวจเวลาขอใหม่ (60 วินาที) */
    Optional<OtpVerification> findTopByUserIdAndPurposeOrderByCreatedAtDesc(Long userId, String purpose);
}
