package com.example.demo.exception;

import com.example.demo.dto.FieldErrorDTO;
import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * Exception ที่ Service โยนเมื่อทำรายการไม่ได้ GlobalExceptionHandler จะแปลงเป็น response รูปแบบกลาง
 * ใช้ factory method ด้านล่าง เช่น throw ApiException.notFound("Order not found")
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final List<FieldErrorDTO> errors;

    public ApiException(HttpStatus status, String message, List<FieldErrorDTO> errors) {
        super(message);
        this.status = status;
        this.errors = errors;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public List<FieldErrorDTO> getErrors() {
        return errors;
    }

    /** 400 ทั่วไป เช่น OTP ผิด */
    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message, null);
    }

    /** 400 ที่บอก field ที่ผิด เช่น paidAmount ไม่เท่ายอดรวม */
    public static ApiException validation(String field, String fieldMessage) {
        return new ApiException(HttpStatus.BAD_REQUEST, "Validation failed",
                List.of(new FieldErrorDTO(field, fieldMessage)));
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message, null);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message, null);
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message, null);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message, null);
    }

    public static ApiException tooManyRequests(String message) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, message, null);
    }
}
