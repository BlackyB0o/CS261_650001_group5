package com.example.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** POST /api/v1/orders (buyerId มาจาก JWT) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequestDTO {

    @NotNull
    private Long roundId;

    @NotBlank
    @Pattern(regexp = "MEETUP|DELIVERY", message = "must be MEETUP or DELIVERY")
    private String handoffMethod;

    @NotBlank
    private String handoffLocation;

    @NotEmpty
    @Valid
    private List<OrderItemRequestDTO> items;
}
