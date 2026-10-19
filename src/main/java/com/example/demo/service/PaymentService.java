package com.example.demo.service;

import com.example.demo.dto.PaymentResponseDTO;
import com.example.demo.dto.SetProductPriceRequestDTO;
import com.example.demo.dto.SubmitSlipRequestDTO;

/** Task20 */
public interface PaymentService {

    /** เจ้าของรอบเท่านั้น */
    PaymentResponseDTO setProductPrice(Long runnerId, Long orderId, SetProductPriceRequestDTO request);

    /** เจ้าของออเดอร์ หรือเจ้าของรอบ */
    PaymentResponseDTO getPayment(Long userId, Long orderId);

    /** เจ้าของออเดอร์เท่านั้น เมื่อสำเร็จ Payment = HELD และ Order = PAID */
    PaymentResponseDTO submitSlip(Long buyerId, Long orderId, SubmitSlipRequestDTO request);
}
