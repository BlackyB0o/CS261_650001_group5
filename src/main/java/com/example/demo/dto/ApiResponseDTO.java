package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Getter;

/**
 * รูปแบบ response กลางของทุก endpoint ดู docs/common/API-CONVENTIONS.md
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponseDTO<T> {

    private String status;
    private String message;
    private T data;
    private List<FieldErrorDTO> errors;

    public ApiResponseDTO(String status, String message, T data, List<FieldErrorDTO> errors) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.errors = errors;
    }

    /** สำเร็จแบบมี message (POST/PATCH/PUT) */
    public static <T> ApiResponseDTO<T> success(String message, T data) {
        return new ApiResponseDTO<>("SUCCESS", message, data, null);
    }

    /** สำเร็จแบบไม่มี message (GET) */
    public static <T> ApiResponseDTO<T> success(T data) {
        return new ApiResponseDTO<>("SUCCESS", null, data, null);
    }

    /** สำเร็จแบบไม่มี data */
    public static <T> ApiResponseDTO<T> success(String message) {
        return new ApiResponseDTO<>("SUCCESS", message, null, null);
    }

    public static <T> ApiResponseDTO<T> error(String message) {
        return new ApiResponseDTO<>("ERROR", message, null, null);
    }

    public static <T> ApiResponseDTO<T> error(String message, List<FieldErrorDTO> errors) {
        return new ApiResponseDTO<>("ERROR", message, null, errors);
    }
}
