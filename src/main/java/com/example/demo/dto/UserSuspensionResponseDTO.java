package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Task54 (US10) ผลของ PATCH /api/v1/admin/users/{userId}/suspend */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSuspensionResponseDTO {

    private Long userId;
    private String fullName;
    private Boolean suspended;
}
