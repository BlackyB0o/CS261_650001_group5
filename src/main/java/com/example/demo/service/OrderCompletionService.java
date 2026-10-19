package com.example.demo.service;

import com.example.demo.dto.CompletionResponseDTO;
import com.example.demo.dto.ConfirmReceiptRequestDTO;

/** Task24 */
public interface OrderCompletionService {

    /** เจ้าของออเดอร์เท่านั้น เมื่อสำเร็จ Payment = RELEASED และ Order = COMPLETED */
    CompletionResponseDTO confirmReceipt(Long buyerId, Long orderId, ConfirmReceiptRequestDTO request);

    /** เจ้าของออเดอร์ หรือเจ้าของรอบ */
    CompletionResponseDTO getCompletion(Long userId, Long orderId);
}
