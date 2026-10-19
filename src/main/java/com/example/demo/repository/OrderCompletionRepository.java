package com.example.demo.repository;

import com.example.demo.entity.OrderCompletion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderCompletionRepository extends JpaRepository<OrderCompletion, Long> {

    /** ค้นผ่าน payment.order.id */
    Optional<OrderCompletion> findByPaymentOrderId(Long orderId);

    /** กันการยืนยันซ้ำ */
    boolean existsByPaymentId(Long paymentId);
}
