package com.example.demo.controller;

import com.example.demo.common.RequestAttributes;
import com.example.demo.dto.ApiResponseDTO;
import com.example.demo.dto.CompletionResponseDTO;
import com.example.demo.dto.ConfirmReceiptRequestDTO;
import com.example.demo.service.OrderCompletionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Task24 ยืนยันรับสินค้าและปิดออเดอร์ ทุก endpoint ต้องมี token (userId จาก JWT) */
@RestController
@RequestMapping("/api/v1/orders/{orderId}/completion")
@RequiredArgsConstructor
public class OrderCompletionController {

    private final OrderCompletionService orderCompletionService;

    /** ผู้ฝากหิ้ว (เจ้าของออเดอร์) ยืนยันรับสินค้าพร้อมรูป multipart/form-data: evidenceImage */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseDTO<CompletionResponseDTO>> confirmReceipt(
            @RequestAttribute(RequestAttributes.USER_ID) Long userId,
            @PathVariable Long orderId,
            @Valid @ModelAttribute ConfirmReceiptRequestDTO request) {
        CompletionResponseDTO data = orderCompletionService.confirmReceipt(userId, orderId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success("Order completed successfully", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<CompletionResponseDTO>> getCompletion(
            @RequestAttribute(RequestAttributes.USER_ID) Long userId,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponseDTO.success(orderCompletionService.getCompletion(userId, orderId)));
    }
}
