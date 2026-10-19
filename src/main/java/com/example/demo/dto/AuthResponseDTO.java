package com.example.demo.dto;

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
public class AuthResponseDTO {

    /** JWT มีเฉพาะ userId ไม่มี role */
    private String token;
    /** "Bearer" */
    private String tokenType;
    private Long userId;
    private String fullName;
}
