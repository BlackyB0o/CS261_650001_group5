package com.example.demo.controller;

import com.example.demo.common.RequestAttributes;
import com.example.demo.dto.ApiResponseDTO;
import com.example.demo.dto.PaymentResponseDTO;
import com.example.demo.dto.SetProductPriceRequestDTO;
import com.example.demo.dto.SubmitSlipRequestDTO;
import com.example.demo.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Task20 การชำระเงินของออเดอร์ ทุก endpoint ต้องมี token (userId จาก JWT) */
@RestController
@RequestMapping("/api/v1/orders/{orderId}")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** ผู้รับหิ้ว (เจ้าของรอบ) ระบุราคาสินค้า */
    @PutMapping("/price")
    public ResponseEntity<ApiResponseDTO<PaymentResponseDTO>> setProductPrice(
            @RequestAttribute(RequestAttributes.USER_ID) Long userId,
            @PathVariable Long orderId,
            @Valid @RequestBody SetProductPriceRequestDTO request) {
        PaymentResponseDTO data = paymentService.setProductPrice(userId, orderId, request);
        return ResponseEntity.ok(ApiResponseDTO.success("Product price set successfully", data));
    }

    @GetMapping("/payment")
    public ResponseEntity<ApiResponseDTO<PaymentResponseDTO>> getPayment(
            @RequestAttribute(RequestAttributes.USER_ID) Long userId,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponseDTO.success(paymentService.getPayment(userId, orderId)));
    }

    /** ผู้ฝากหิ้ว (เจ้าของออเดอร์) แนบสลิป multipart/form-data: slipImage + paidAmount */
    @PostMapping(value = "/payment/slip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseDTO<PaymentResponseDTO>> submitSlip(
            @RequestAttribute(RequestAttributes.USER_ID) Long userId,
            @PathVariable Long orderId,
            @Valid @ModelAttribute SubmitSlipRequestDTO request) {
        PaymentResponseDTO data = paymentService.submitSlip(userId, orderId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success("Payment confirmed successfully", data));
    }
}
