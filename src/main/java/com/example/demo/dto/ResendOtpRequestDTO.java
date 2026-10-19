package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** POST /api/v1/auth/otp/resend */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResendOtpRequestDTO {

    @NotNull
    private Long verificationId;
}
