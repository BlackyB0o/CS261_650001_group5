package com.example.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** POST /api/v1/posts (buyerId มาจาก JWT) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostRequestDTO {

    @NotBlank
    @Size(max = 255, message = "must not exceed 255 characters")
    private String postTitle;

    @NotBlank
    @Size(max = 255, message = "must not exceed 255 characters")
    private String shopName;

    @NotBlank
    private String productDetail;

    @NotNull
    @Min(1)
    private Integer quantity;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal carryFee;

    @NotNull
    @FutureOrPresent(message = "must be today or a future date")
    private LocalDate desiredDate;

    @NotBlank
    @Size(max = 255, message = "must not exceed 255 characters")
    private String location;

    /** ไม่บังคับ */
    @DecimalMin("0.00")
    private BigDecimal budget;

    /** ไม่บังคับ */
    private String additionalDetails;
}
