package com.example.demo.service;

import com.example.demo.dto.AuthResponseDTO;
import com.example.demo.dto.ForgotPasswordRequestDTO;
import com.example.demo.dto.LoginRequestDTO;
import com.example.demo.dto.OtpSentResponseDTO;
import com.example.demo.dto.RegisterRequestDTO;
import com.example.demo.dto.RegisterResponseDTO;
import com.example.demo.dto.ResendOtpRequestDTO;
import com.example.demo.dto.ResetPasswordRequestDTO;
import com.example.demo.dto.VerifyOtpRequestDTO;

/** Task04 ข้อผิดพลาดโยนด้วย ApiException (ดู docs/task-04/README.md) */
public interface AuthService {

    RegisterResponseDTO register(RegisterRequestDTO request);

    OtpSentResponseDTO login(LoginRequestDTO request);

    AuthResponseDTO verifyOtp(VerifyOtpRequestDTO request);

    OtpSentResponseDTO resendOtp(ResendOtpRequestDTO request);

    OtpSentResponseDTO forgotPassword(ForgotPasswordRequestDTO request);

    void resetPassword(ResetPasswordRequestDTO request);
}
