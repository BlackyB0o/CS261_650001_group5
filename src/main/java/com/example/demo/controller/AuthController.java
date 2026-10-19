package com.example.demo.controller;

import com.example.demo.dto.ApiResponseDTO;
import com.example.demo.dto.AuthResponseDTO;
import com.example.demo.dto.ForgotPasswordRequestDTO;
import com.example.demo.dto.LoginRequestDTO;
import com.example.demo.dto.OtpSentResponseDTO;
import com.example.demo.dto.RegisterRequestDTO;
import com.example.demo.dto.RegisterResponseDTO;
import com.example.demo.dto.ResendOtpRequestDTO;
import com.example.demo.dto.ResetPasswordRequestDTO;
import com.example.demo.dto.VerifyOtpRequestDTO;
import com.example.demo.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Task04 ทุก endpoint ไม่ต้องใช้ token (Guest) */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO<RegisterResponseDTO>> register(
            @Valid @RequestBody RegisterRequestDTO request) {
        RegisterResponseDTO data = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success("User registered successfully", data));
    }

    /** ขั้นที่ 1 ตรวจรหัสผ่านและส่ง OTP (ยังไม่ส่ง token) */
    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<OtpSentResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request) {
        OtpSentResponseDTO data = authService.login(request);
        return ResponseEntity.ok(ApiResponseDTO.success("OTP sent to your phone number", data));
    }

    /** ขั้นที่ 2 ยืนยัน OTP และรับ JWT */
    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequestDTO request) {
        AuthResponseDTO data = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponseDTO.success("Login successful", data));
    }

    @PostMapping("/otp/resend")
    public ResponseEntity<ApiResponseDTO<OtpSentResponseDTO>> resendOtp(
            @Valid @RequestBody ResendOtpRequestDTO request) {
        OtpSentResponseDTO data = authService.resendOtp(request);
        return ResponseEntity.ok(ApiResponseDTO.success("OTP sent to your phone number", data));
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<ApiResponseDTO<OtpSentResponseDTO>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO request) {
        OtpSentResponseDTO data = authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponseDTO.success("OTP sent to your phone number", data));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponseDTO<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponseDTO.success("Password reset successfully"));
    }
}
