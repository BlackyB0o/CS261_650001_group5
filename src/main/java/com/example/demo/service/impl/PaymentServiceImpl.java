package com.example.demo.service.impl;

import com.example.demo.dto.PaymentResponseDTO;
import com.example.demo.dto.SetProductPriceRequestDTO;
import com.example.demo.dto.SubmitSlipRequestDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.Payment;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.PaymentService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Task20 ผู้รับหิ้วระบุราคา -> ระบบออก QR ของระบบตัวกลาง -> ผู้ฝากหิ้วแนบสลิป
 * สลิปผ่าน: Payment = HELD (เงินพักที่ระบบกลาง), Order = PAID
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final DateTimeFormatter TXN_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public PaymentResponseDTO setProductPrice(Long runnerId, Long orderId, SetProductPriceRequestDTO request) {
        Order order = findOrder(orderId, "Order not found");
        if (!order.getRound().getRunner().getId().equals(runnerId)) {
            throw ApiException.forbidden("You are not the owner of this round");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw ApiException.conflict("Order has already been paid");
        }
        Payment payment = paymentRepository.findByOrderId(orderId).orElseGet(Payment::new);
        BigDecimal productAmount = request.getProductAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal serviceFee = order.getRound().getServiceFee().setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = productAmount.add(serviceFee);
        payment.setOrder(order);
        payment.setProductAmount(productAmount);
        payment.setServiceFee(serviceFee);
        payment.setTotalAmount(total);
        payment.setPaymentMethod("QR_TRANSFER");
        payment.setPaymentStatus("PENDING");
        payment = paymentRepository.save(payment);
        payment.setQrPayload("CARRYHUB-PAY|" + payment.getId() + "|" + total.toPlainString());
        return toDto(paymentRepository.save(payment));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPayment(Long userId, Long orderId) {
        Order order = findOrder(orderId, "Payment not found");
        boolean isBuyer = order.getBuyer().getId().equals(userId);
        boolean isRunner = order.getRound().getRunner().getId().equals(userId);
        if (!isBuyer && !isRunner) {
            throw ApiException.forbidden("You are not allowed to view this payment");
        }
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> ApiException.notFound("Payment not found"));
        return toDto(payment);
    }

    @Override
    @Transactional
    public PaymentResponseDTO submitSlip(Long buyerId, Long orderId, SubmitSlipRequestDTO request) {
        Order order = findOrder(orderId, "Order not found");
        if (!order.getBuyer().getId().equals(buyerId)) {
            throw ApiException.forbidden("You are not the owner of this order");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw ApiException.conflict("Order has already been paid");
        }
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> ApiException.conflict("Product price has not been set yet"));
        if (!"PENDING".equals(payment.getPaymentStatus())) {
            throw ApiException.conflict("Order has already been paid");
        }
        if (request.getPaidAmount().compareTo(payment.getTotalAmount()) != 0) {
            throw ApiException.validation("paidAmount",
                    "must equal the total amount (" + payment.getTotalAmount().toPlainString() + ")");
        }
        String slipUrl = fileStorageService.store(request.getSlipImage(), "slips");
        LocalDateTime now = LocalDateTime.now();
        payment.setSlipImageUrl(slipUrl);
        payment.setTransactionReference(
                "TXN-" + now.format(TXN_DATE) + "-" + String.format("%06d", payment.getId()));
        payment.setPaidAt(now);
        payment.setPaymentStatus("HELD");
        order.setStatus("PAID");
        orderRepository.save(order);
        return toDto(paymentRepository.save(payment));
    }

    private Order findOrder(Long orderId, String notFoundMessage) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound(notFoundMessage));
    }

    private static PaymentResponseDTO toDto(Payment payment) {
        return PaymentResponseDTO.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrder().getId())
                .productAmount(payment.getProductAmount())
                .serviceFee(payment.getServiceFee())
                .totalAmount(payment.getTotalAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .qrPayload(payment.getQrPayload())
                .slipImageUrl(payment.getSlipImageUrl())
                .transactionReference(payment.getTransactionReference())
                .paidAt(payment.getPaidAt())
                .orderStatus(payment.getOrder().getStatus())
                .build();
    }
}
