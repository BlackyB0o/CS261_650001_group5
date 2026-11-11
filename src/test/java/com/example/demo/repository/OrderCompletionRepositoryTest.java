package com.example.demo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.entity.OrderCompletion;
import com.example.demo.entity.User;
import com.example.demo.entity.Payment;
import com.example.demo.entity.Order;
import com.example.demo.entity.DeliveryRound;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class OrderCompletionRepositoryTest {

    @Autowired
    private OrderCompletionRepository repository;

    @Autowired
    private TestEntityManager em;

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 10, 25, 12, 0);

    @Test
    @DisplayName("DAO-U12-01 save: บันทึกการยืนยันคำสั่งซื้อสำเร็จและอ่านค่ากลับได้ตรงกัน")
    void daoU12_01_save_persistsAndFindsSuccessfully() {
        User user = persistUser("user1@example.com", "0811111111", "User One");
        DeliveryRound round = persistRound(user, "Starbucks");
        Order order = persistOrder(user, round);
        Payment payment = persistPayment(order);

        OrderCompletion completion = new OrderCompletion();
        completion.setPayment(payment);
        completion.setConfirmedBy(user); // ส่งเป็น User Object
        completion.setConfirmedAt(T0);
        completion.setCompletionStatus("COMPLETED");
        completion.setEvidenceImageUrl("http://example.com/slip.jpg");

        OrderCompletion saved = repository.save(completion);
        em.clear();

        Optional<OrderCompletion> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getCompletionStatus()).isEqualTo("COMPLETED");
        assertThat(found.get().getEvidenceImageUrl()).isEqualTo("http://example.com/slip.jpg");
        assertThat(found.get().getConfirmedBy().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("DAO-U12-02 save: ไม่ใส่ evidence_image_url ต้องบันทึกไม่ได้ (Violation)")
    void daoU12_02_missingEvidenceImage_throws() {
        User user = persistUser("user2@example.com", "0822222222", "User Two");
        DeliveryRound round = persistRound(user, "Cafe Amazon");
        Order order = persistOrder(user, round);
        Payment payment = persistPayment(order);

        OrderCompletion completion = new OrderCompletion();
        completion.setPayment(payment);
        completion.setConfirmedBy(user); // ส่งเป็น User Object
        completion.setConfirmedAt(T0);
        completion.setCompletionStatus("COMPLETED");
        // ไม่กำหนด evidenceImageUrl ซึ่งเป็น NOT NULL

        assertThatThrownBy(() -> repository.saveAndFlush(completion))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // --- Helper Methods ---
    private User persistUser(String email, String phone, String name) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("hash");
        u.setFullName(name);
        u.setPhoneNumber(phone);
        return em.persistAndFlush(u);
    }

    private DeliveryRound persistRound(User runner, String storeName) {
        DeliveryRound r = new DeliveryRound();
        r.setRunner(runner);
        r.setStoreName(storeName);
        r.setLocationName("SC1");
        r.setScheduledAt(T0);
        r.setServiceFee(BigDecimal.TEN);
        r.setStatus("OPEN");
        return em.persistAndFlush(r);
    }

    private Order persistOrder(User buyer, DeliveryRound round) {
        Order o = new Order();
        o.setBuyer(buyer); // ใช้ setBuyer แทน setBuyerId
        o.setRound(round);
        o.setStatus("PENDING");
        o.setHandoffMethod("PICKUP");
        o.setHandoffLocation("SC1");
        return em.persistAndFlush(o);
    }

    private Payment persistPayment(Order order) {
        Payment p = new Payment();
        p.setOrder(order);
        p.setPaymentMethod("QR");
        p.setPaymentStatus("PAID");
        p.setProductAmount(BigDecimal.valueOf(100));
        p.setServiceFee(BigDecimal.TEN);
        p.setTotalAmount(BigDecimal.valueOf(110));
        return em.persistAndFlush(p);
    }
}