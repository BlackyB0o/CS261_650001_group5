package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** PATCH /api/v1/delivery-rounds/{roundId}/status */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoundStatusRequestDTO {

    @NotBlank
    @Pattern(regexp = "OPEN|CLOSED|COMPLETED|CANCELLED",
            message = "must be one of OPEN, CLOSED, COMPLETED, CANCELLED")
    private String status;
}
