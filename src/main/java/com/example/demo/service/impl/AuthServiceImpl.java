package com.example.demo.service.impl;

import com.example.demo.dto.AuthResponseDTO;
import com.example.demo.dto.ForgotPasswordRequestDTO;
import com.example.demo.dto.LoginRequestDTO;
import com.example.demo.dto.OtpSentResponseDTO;
import com.example.demo.dto.RegisterRequestDTO;
import com.example.demo.dto.RegisterResponseDTO;
import com.example.demo.dto.ResendOtpRequestDTO;
import com.example.demo.dto.ResetPasswordRequestDTO;
import com.example.demo.dto.VerifyOtpRequestDTO;
import com.example.demo.entity.OtpVerification;
import com.example.demo.entity.User;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.OtpVerificationRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import com.example.demo.security.PasswordHasher;
import com.example.demo.service.AuthService;
import com.example.demo.service.SmsSender;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Task04 สมัครสมาชิก, login 2 ขั้นตอน (รหัสผ่าน -> OTP -> JWT), ขอ OTP ใหม่, ลืมรหัสผ่าน
 * กติกา OTP: 6 หลัก, หมดอายุ 5 นาที, กรอกผิดได้ 5 ครั้ง, ขอใหม่ได้ทุก 60 วินาที, เก็บแบบ hash
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    static final String PURPOSE_LOGIN = "LOGIN";
    static final String PURPOSE_RESET = "RESET_PASSWORD";
    private static final int OTP_EXPIRE_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpRepository;
    private final PasswordHasher passwordHasher;
    private final JwtService jwtService;
    private final SmsSender smsSender;
    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public RegisterResponseDTO register(RegisterRequestDTO request) {
        String email = request.getEmail().trim().toLowerCase();
        String phone = request.getPhoneNumber().trim();
        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("Email already exists");
        }
        if (userRepository.existsByPhoneNumber(phone)) {
            throw ApiException.conflict("Phone number already exists");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordHasher.hash(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(phone);
        user = userRepository.save(user);
        return RegisterResponseDTO.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    @Override
    @Transactional
    public OtpSentResponseDTO login(LoginRequestDTO request) {
        String identifier = request.getIdentifier().trim();
        User user = userRepository.findByEmailOrPhoneNumber(identifier.toLowerCase(), identifier)
                .orElseThrow(() -> ApiException.unauthorized("Invalid credentials"));
        if (!passwordHasher.matches(request.getPassword(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid credentials");
        }
        return issueOtp(user, PURPOSE_LOGIN);
    }

    /** noRollbackFor: จำนวนครั้งที่กรอกผิดต้องถูกบันทึกแม้จะตอบ error */
    @Override
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponseDTO verifyOtp(VerifyOtpRequestDTO request) {
        OtpVerification otp = checkOtp(request.getVerificationId(), request.getOtp(), PURPOSE_LOGIN);
        User user = otp.getUser();
        return AuthResponseDTO.builder()
                .token(jwtService.generateToken(user.getId()))
                .tokenType("Bearer")
                .userId(user.getId())
                .fullName(user.getFullName())
                .build();
    }

    @Override
    @Transactional
    public OtpSentResponseDTO resendOtp(ResendOtpRequestDTO request) {
        OtpVerification old = otpRepository.findById(request.getVerificationId())
                .orElseThrow(() -> ApiException.notFound("Verification not found"));
        User user = old.getUser();
        otpRepository.findTopByUserIdAndPurposeOrderByCreatedAtDesc(user.getId(), old.getPurpose())
                .ifPresent(latest -> {
                    if (latest.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(RESEND_COOLDOWN_SECONDS))) {
                        throw ApiException.tooManyRequests("Please wait before requesting a new OTP");
                    }
                });
        return issueOtp(user, old.getPurpose());
    }

    @Override
    @Transactional
    public OtpSentResponseDTO forgotPassword(ForgotPasswordRequestDTO request) {
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber().trim())
                .orElseThrow(() -> ApiException.notFound("Phone number is not registered"));
        return issueOtp(user, PURPOSE_RESET);
    }

    @Override
    @Transactional(noRollbackFor = ApiException.class)
    public void resetPassword(ResetPasswordRequestDTO request) {
        OtpVerification otp = checkOtp(request.getVerificationId(), request.getOtp(), PURPOSE_RESET);
        User user = otp.getUser();
        user.setPasswordHash(passwordHasher.hash(request.getNewPassword()));
        userRepository.save(user);
    }

    /** สร้าง OTP ใหม่ บันทึกแบบ hash แล้วส่ง SMS (mock) */
    private OtpSentResponseDTO issueOtp(User user, String purpose) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        OtpVerification otp = new OtpVerification();
        otp.setUser(user);
        otp.setOtpHash(passwordHasher.sha256(code));
        otp.setPurpose(purpose);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRE_MINUTES));
        otp.setAttemptsCount(0);
        otp = otpRepository.save(otp);
        smsSender.send(user.getPhoneNumber(), "CarryHub OTP: " + code + " (valid " + OTP_EXPIRE_MINUTES
                + " minutes, ref " + otp.getId() + ")");
        return OtpSentResponseDTO.builder()
                .verificationId(otp.getId())
                .maskedPhoneNumber(mask(user.getPhoneNumber()))
                .expiresAt(otp.getExpiresAt())
                .build();
    }

    /** ตรวจ OTP ตามกติกา ถ้าถูกต้องบันทึกเวลาที่ยืนยันแล้วคืนรายการนั้น */
    private OtpVerification checkOtp(Long verificationId, String code, String purpose) {
        OtpVerification otp = otpRepository.findById(verificationId)
                .orElseThrow(() -> ApiException.notFound("Verification not found"));
        if (!purpose.equals(otp.getPurpose()) || otp.getVerifiedAt() != null) {
            throw ApiException.badRequest("Invalid OTP");
        }
        if (otp.getAttemptsCount() >= MAX_ATTEMPTS) {
            throw ApiException.tooManyRequests("Too many attempts. Please request a new OTP");
        }
        boolean isLatest = otpRepository
                .findTopByUserIdAndPurposeOrderByCreatedAtDesc(otp.getUser().getId(), purpose)
                .map(latest -> latest.getId().equals(otp.getId()))
                .orElse(false);
        if (!isLatest || otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw ApiException.badRequest("OTP has expired");
        }
        boolean correct = MessageDigest.isEqual(
                otp.getOtpHash().getBytes(StandardCharsets.UTF_8),
                passwordHasher.sha256(code).getBytes(StandardCharsets.UTF_8));
        if (!correct) {
            otp.setAttemptsCount(otp.getAttemptsCount() + 1);
            otpRepository.save(otp);
            if (otp.getAttemptsCount() >= MAX_ATTEMPTS) {
                throw ApiException.tooManyRequests("Too many attempts. Please request a new OTP");
            }
            throw ApiException.badRequest("Invalid OTP");
        }
        otp.setVerifiedAt(LocalDateTime.now());
        return otpRepository.save(otp);
    }

    /** 0812345678 -> 081-xxx-5678 */
    private static String mask(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "-xxx-" + phone.substring(phone.length() - 4);
    }
}
