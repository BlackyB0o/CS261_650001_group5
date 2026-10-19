package com.example.demo.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpSentResponseDTO {

    private Long verificationId;
    /** เช่น 081-xxx-5678 */
    private String maskedPhoneNumber;
    private LocalDateTime expiresAt;
}
