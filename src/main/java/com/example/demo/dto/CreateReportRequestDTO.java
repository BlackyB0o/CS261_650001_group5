package com.example.demo.dto;

import com.example.demo.common.ReportCategories;
import com.example.demo.validation.ImageFile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/** POST /api/v1/reports (Task51 US09) multipart/form-data ผูกด้วย @ModelAttribute */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateReportRequestDTO {

    @NotNull
    private Long orderId;

    @NotBlank
    @Pattern(regexp = ReportCategories.ALL,
            message = "must be one of PRODUCT_MISMATCH, DELIVERY_ISSUE, PAYMENT_ISSUE, SUSPICIOUS_BEHAVIOR, OTHER")
    private String category;

    @NotBlank
    @Size(max = 1000)
    private String description;

    /** ไม่แนบก็ได้ ถ้าแนบต้องเป็น JPEG/PNG ไม่เกิน 5 MB */
    @ImageFile(required = false)
    private MultipartFile evidenceImage;
}
