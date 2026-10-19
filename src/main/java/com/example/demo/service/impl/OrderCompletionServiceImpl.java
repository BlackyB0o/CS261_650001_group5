package com.example.demo.service.impl;

import com.example.demo.dto.CompletionResponseDTO;
import com.example.demo.dto.ConfirmReceiptRequestDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderCompletion;
import com.example.demo.entity.Payment;
import com.example.demo.entity.User;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.OrderCompletionRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.OrderCompletionService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Task24 ผู้ฝากหิ้วยืนยันรับสินค้าพร้อมรูป (ใน transaction เดียว)
 * สำเร็จ: บันทึก OrderCompletion, Payment = RELEASED (โอนให้ผู้รับหิ้ว), Order = COMPLETED
 */
@Service
@RequiredArgsConstructor
public class OrderCompletionServiceImpl implements OrderCompletionService {

    private final OrderCompletionRepository completionRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public CompletionResponseDTO confirmReceipt(Long buyerId, Long orderId, ConfirmReceiptRequestDTO request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        if (!order.getBuyer().getId().equals(buyerId)) {
            throw ApiException.forbidden("You are not the owner of this order");
        }
        if ("COMPLETED".equals(order.getStatus())) {
            throw ApiException.conflict("Order is already completed");
        }
        if (!"PAID".equals(order.getStatus())) {
            throw ApiException.conflict("Order has not been paid");
        }
        Payment payment = paymentRepository.findByOrderId(orderId)
                .filter(p -> "HELD".equals(p.getPaymentStatus()))
                .orElseThrow(() -> ApiException.conflict("Order has not been paid"));
        if (completionRepository.existsByPaymentId(payment.getId())) {
            throw ApiException.conflict("Order is already completed");
        }
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> ApiException.unauthorized("Invalid or missing token"));

        String imageUrl = fileStorageService.store(request.getEvidenceImage(), "completions");
        OrderCompletion completion = new OrderCompletion();
        completion.setPayment(payment);
        completion.setConfirmedBy(buyer);
        completion.setEvidenceImageUrl(imageUrl);
        completion.setConfirmedAt(LocalDateTime.now());
        completion.setCompletionStatus("COMPLETED");
        completion = completionRepository.save(completion);

        payment.setPaymentStatus("RELEASED");
        paymentRepository.save(payment);
        order.setStatus("COMPLETED");
        orderRepository.save(order);
        return toDto(completion);
    }

    @Override
    @Transactional(readOnly = true)
    public CompletionResponseDTO getCompletion(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order completion not found"));
        boolean isBuyer = order.getBuyer().getId().equals(userId);
        boolean isRunner = order.getRound().getRunner().getId().equals(userId);
        if (!isBuyer && !isRunner) {
            throw ApiException.forbidden("You are not allowed to view this order");
        }
        OrderCompletion completion = completionRepository.findByPaymentOrderId(orderId)
                .orElseThrow(() -> ApiException.notFound("Order completion not found"));
        return toDto(completion);
    }

    private static CompletionResponseDTO toDto(OrderCompletion completion) {
        Payment payment = completion.getPayment();
        Order order = payment.getOrder();
        return CompletionResponseDTO.builder()
                .completionId(completion.getId())
                .orderId(order.getId())
                .paymentId(payment.getId())
                .confirmedBy(completion.getConfirmedBy().getId())
                .confirmedAt(completion.getConfirmedAt())
                .completionStatus(completion.getCompletionStatus())
                .evidenceImageUrl(completion.getEvidenceImageUrl())
                .runnerId(order.getRound().getRunner().getId())
                .releasedAmount(payment.getTotalAmount())
                .orderStatus(order.getStatus())
                .paymentStatus(payment.getPaymentStatus())
                .build();
    }
}
