package com.example.demo.dto;

import com.example.demo.validation.ImageFile;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/** POST /api/v1/orders/{orderId}/payment/slip (multipart/form-data ผูกด้วย @ModelAttribute) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubmitSlipRequestDTO {

    @ImageFile
    private MultipartFile slipImage;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    private BigDecimal paidAmount;
}
