package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** POST /api/v1/auth/otp/verify */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpRequestDTO {

    @NotNull
    private Long verificationId;

    @NotBlank
    @Pattern(regexp = "^[0-9]{6}$", message = "must be a 6-digit number")
    private String otp;
}
