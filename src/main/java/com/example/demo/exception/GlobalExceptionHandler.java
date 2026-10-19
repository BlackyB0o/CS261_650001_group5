package com.example.demo.exception;

import com.example.demo.dto.ApiResponseDTO;
import com.example.demo.dto.FieldErrorDTO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * แปลงทุก error ให้เป็นรูปแบบกลาง { status: "ERROR", message, errors? }
 * ดู docs/common/API-CONVENTIONS.md ข้อ 3
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String VALIDATION_FAILED = "Validation failed";

    /** error ที่ Service โยนเอง (404, 403, 409, 429 ...) */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponseDTO.error(ex.getMessage(), ex.getErrors()));
    }

    /** error ที่ไม่คาดคิด */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleUnexpected(Exception ex) {
        if (ex instanceof MaxUploadSizeExceededException) {
            return ResponseEntity.badRequest().body(tooLarge());
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO.error("Internal server error"));
    }

    /** Bean Validation ไม่ผ่าน (@RequestBody และ multipart @ModelAttribute) -> 400 พร้อม errors */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return body(HttpStatus.BAD_REQUEST, ApiResponseDTO.error(VALIDATION_FAILED, toFieldErrors(ex)));
    }

    /** JSON พัง / ค่า enum หรือวันที่ผิดรูปแบบ -> 400 */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return body(HttpStatus.BAD_REQUEST, ApiResponseDTO.error("Malformed or invalid request body"));
    }

    /** ขาด query parameter บังคับ เช่น roundId -> 400 "roundId is required" */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {
        return body(HttpStatus.BAD_REQUEST, ApiResponseDTO.error(ex.getParameterName() + " is required"));
    }

    /** ไม่มี userId จาก JWT filter = ไม่มี token หรือ token ไม่ถูกต้อง -> 401 */
    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(ServletRequestBindingException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // Spring ไม่มี exception แยกสำหรับ @RequestAttribute ที่หายไป จึงดูจากข้อความ
        // "Missing request attribute 'userId' ..." = JWT filter ไม่ได้ตั้ง userId (ไม่มี token หรือ token ผิด)
        String detail = ex.getMessage() == null ? "" : ex.getMessage();
        if (detail.contains("request attribute")) {
            return body(HttpStatus.UNAUTHORIZED, ApiResponseDTO.error("Invalid or missing token"));
        }
        return body(HttpStatus.BAD_REQUEST, ApiResponseDTO.error(VALIDATION_FAILED));
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return body(HttpStatus.METHOD_NOT_ALLOWED, ApiResponseDTO.error("Method not allowed"));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return body(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ApiResponseDTO.error("Unsupported content type"));
    }

    /** error อื่นของ Spring MVC (type mismatch, ไม่พบ path ฯลฯ) ใช้รูปแบบกลางเหมือนกัน */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (ex instanceof MaxUploadSizeExceededException) {
            return body(HttpStatus.BAD_REQUEST, tooLarge());
        }
        String message = statusCode.value() == 404 ? "Resource not found" : VALIDATION_FAILED;
        if (statusCode.is5xxServerError()) {
            message = "Internal server error";
        }
        return body(HttpStatus.valueOf(statusCode.value()), ApiResponseDTO.error(message));
    }

    /** ไฟล์ใหญ่เกินที่ตั้งใน spring.servlet.multipart.max-file-size (5 MB) */
    private ApiResponseDTO<Void> tooLarge() {
        return ApiResponseDTO.error(VALIDATION_FAILED,
                List.of(new FieldErrorDTO("file", "file size must not exceed 5 MB")));
    }

    private ResponseEntity<Object> body(HttpStatus status, ApiResponseDTO<Void> body) {
        return ResponseEntity.status(status).body(body);
    }

    /**
     * แปลง error ของ Bean Validation เป็นรายการ field
     * หนึ่ง field แสดงหนึ่ง message (ถ้าว่างหรือ null ให้แสดง "must not be ..." ก่อน)
     */
    private List<FieldErrorDTO> toFieldErrors(BindException ex) {
        Map<String, String> byField = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            String msg = fe.getDefaultMessage() == null ? "invalid value" : fe.getDefaultMessage();
            String old = byField.get(fe.getField());
            if (old == null || (msg.startsWith("must not be") && !old.startsWith("must not be"))) {
                byField.put(fe.getField(), msg);
            }
        }
        List<FieldErrorDTO> result = new ArrayList<>();
        byField.forEach((f, m) -> result.add(new FieldErrorDTO(f, m)));
        return result;
    }
}
