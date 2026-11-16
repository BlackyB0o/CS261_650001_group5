package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Task54 (US10) PATCH /api/v1/admin/users/{userId}/suspend */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SuspendUserRequestDTO {

    /** true = ระงับบัญชี, false = คืนสิทธิ์บัญชี */
    @NotNull
    private Boolean suspended;
}
