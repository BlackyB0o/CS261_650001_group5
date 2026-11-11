package com.example.demo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.entity.DeliveryRound;
import com.example.demo.entity.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Task09:U01 - Unit test ส่วน DAO ของ US01 (เปิดรอบรับหิ้ว)
 *
 * ทดสอบ DeliveryRoundRepository กับ H2 ในหน่วยความจำ (ไม่ต้องมี PostgreSQL)
 * แต่ละเทสต์รันใน transaction แล้ว rollback ให้เอง
 * ข้อมูลจึงไม่ปนกันระหว่างเทสต์
 */
@DataJpaTest
class DeliveryRoundRepositoryTest {

    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 10, 25, 12, 0);

    @Autowired
    private DeliveryRoundRepository repository;

    @Autowired
    private TestEntityManager em;

    // ---------- save / findById ----------

    @Test
    @DisplayName("DAO-01 save: รอบใหม่ได้ id และสถานะเริ่มต้นเป็น OPEN")
    void dao01_save_generatesIdAndDefaultsToOpen() {
        User runner = persistUser();
        DeliveryRound round = new DeliveryRound();
        round.setRunner(runner);
        round.setStoreName("Starbucks");
        round.setLocationName("หน้าอาคาร SC1");
        round.setScheduledAt(T0);
        round.setServiceFee(new BigDecimal("20.00"));

        DeliveryRound saved = repository.save(round);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("DAO-02 save: บันทึกข้อมูลครบทุกฟิลด์ อ่านกลับมาจาก DB ได้ตรงกัน")
    void dao02_save_persistsAllFields() {
        User runner = persistUser();
        DeliveryRound saved = persistRound(runner, "Starbucks", T0, "OPEN");
        em.clear(); // ล้าง cache เพื่อให้อ่านจาก DB จริง

        DeliveryRound found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getStoreName()).isEqualTo("Starbucks");
        assertThat(found.getLocationName()).isEqualTo("หน้าอาคาร SC1");
        assertThat(found.getScheduledAt()).isEqualTo(T0);
        assertThat(found.getServiceFee()).isEqualByComparingTo("20.00");
        assertThat(found.getStatus()).isEqualTo("OPEN");
        assertThat(found.getRunner().getId()).isEqualTo(runner.getId());
    }

    @Test
    @DisplayName("DAO-03 findById: มีรอบนั้นอยู่ คืนค่ารอบ")
    void dao03_findById_existing_returnsRound() {
        DeliveryRound saved = persistRound(persistUser(), "7-Eleven", T0, "OPEN");
        em.clear();

        Optional<DeliveryRound> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getStoreName()).isEqualTo("7-Eleven");
    }

    @Test
    @DisplayName("DAO-04 findById: ไม่มีรอบนั้น คืนค่าว่าง")
    void dao04_findById_missing_returnsEmpty() {
        assertThat(repository.findById(999_999L)).isEmpty();
    }

    // ---------- findByStatusOrderByScheduledAtAsc (GET /delivery-rounds)
    // ----------

    @Test
    @DisplayName("DAO-05 findByStatus: คืนเฉพาะรอบที่สถานะตรงกัน")
    void dao05_findByStatus_returnsOnlyMatchingStatus() {
        User runner = persistUser();
        persistRound(runner, "OPEN-A", T0, "OPEN");
        persistRound(runner, "CLOSED-A", T0.plusHours(1), "CLOSED");
        persistRound(runner, "CANCELLED-A", T0.plusHours(2), "CANCELLED");
        persistRound(runner, "OPEN-B", T0.plusHours(3), "OPEN");
        em.clear();

        List<DeliveryRound> open = repository.findByStatusOrderByScheduledAtAsc("OPEN");

        assertThat(open).extracting(DeliveryRound::getStoreName)
                .containsExactly("OPEN-A", "OPEN-B");
    }

    @Test
    @DisplayName("DAO-06 findByStatus: เรียงตามเวลานัด ใกล้ไปไกล (ไม่ขึ้นกับลำดับที่บันทึก)")
    void dao06_findByStatus_sortedByScheduledAtAscending() {
        User runner = persistUser();
        persistRound(runner, "late", T0.plusHours(5), "OPEN");
        persistRound(runner, "early", T0, "OPEN");
        persistRound(runner, "middle", T0.plusHours(2), "OPEN");
        em.clear();

        List<DeliveryRound> rounds = repository.findByStatusOrderByScheduledAtAsc("OPEN");

        assertThat(rounds).extracting(DeliveryRound::getStoreName)
                .containsExactly("early", "middle", "late");
    }

    @Test
    @DisplayName("DAO-07 findByStatus: ไม่มีรอบที่สถานะนั้น คืนลิสต์ว่าง")
    void dao07_findByStatus_noMatch_returnsEmptyList() {
        persistRound(persistUser(), "only-open", T0, "OPEN");
        em.clear();

        assertThat(repository.findByStatusOrderByScheduledAtAsc("COMPLETED")).isEmpty();
    }

    // ---------- findByRunnerIdOrderByScheduledAtDesc (GET /delivery-rounds/my)
    // ----------

    @Test
    @DisplayName("DAO-08 findByRunnerId: คืนเฉพาะรอบของผู้รับหิ้วคนนั้น ทุกสถานะ")
    void dao08_findByRunnerId_returnsOnlyThatRunnersRounds() {
        User runnerA = persistUser();
        User runnerB = persistUser();
        persistRound(runnerA, "A-open", T0, "OPEN");
        persistRound(runnerA, "A-closed", T0.plusHours(1), "CLOSED");
        persistRound(runnerB, "B-open", T0.plusHours(2), "OPEN");
        em.clear();

        List<DeliveryRound> rounds = repository.findByRunnerIdOrderByScheduledAtDesc(runnerA.getId());

        assertThat(rounds).extracting(DeliveryRound::getStoreName)
                .containsExactlyInAnyOrder("A-open", "A-closed");
        assertThat(rounds).extracting(r -> r.getRunner().getId())
                .containsOnly(runnerA.getId());
    }

    @Test
    @DisplayName("DAO-09 findByRunnerId: เรียงตามเวลานัด ล่าสุดก่อน")
    void dao09_findByRunnerId_sortedByScheduledAtDescending() {
        User runner = persistUser();
        persistRound(runner, "middle", T0.plusHours(2), "OPEN");
        persistRound(runner, "latest", T0.plusHours(5), "CLOSED");
        persistRound(runner, "earliest", T0, "COMPLETED");
        em.clear();

        List<DeliveryRound> rounds = repository.findByRunnerIdOrderByScheduledAtDesc(runner.getId());

        assertThat(rounds).extracting(DeliveryRound::getStoreName)
                .containsExactly("latest", "middle", "earliest");
    }

    @Test
    @DisplayName("DAO-10 findByRunnerId: ผู้รับหิ้วที่ยังไม่เคยเปิดรอบ หรือไม่มีผู้ใช้นั้น คืนลิสต์ว่าง")
    void dao10_findByRunnerId_noRounds_returnsEmptyList() {
        User runnerWithoutRounds = persistUser();
        em.clear();

        assertThat(repository.findByRunnerIdOrderByScheduledAtDesc(runnerWithoutRounds.getId()))
                .isEmpty();
        assertThat(repository.findByRunnerIdOrderByScheduledAtDesc(999_999L)).isEmpty();
    }

    // ---------- ข้อบังคับของตาราง (NOT NULL) ----------

    @Test
    @DisplayName("DAO-11 save: ไม่ใส่ชื่อร้าน (store_name) ต้องบันทึกไม่ได้")
    void dao11_save_missingStoreName_throws() {
        DeliveryRound round = new DeliveryRound();
        round.setRunner(persistUser());
        round.setLocationName("หน้าอาคาร SC1");
        round.setScheduledAt(T0);
        round.setServiceFee(new BigDecimal("20.00"));

        assertThatThrownBy(() -> repository.saveAndFlush(round))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("DAO-12 save: ไม่ระบุผู้รับหิ้ว (runner_id) ต้องบันทึกไม่ได้")
    void dao12_save_missingRunner_throws() {
        DeliveryRound round = new DeliveryRound();
        round.setStoreName("Starbucks");
        round.setLocationName("หน้าอาคาร SC1");
        round.setScheduledAt(T0);
        round.setServiceFee(new BigDecimal("20.00"));

        assertThatThrownBy(() -> repository.saveAndFlush(round))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- helpers ----------

    /** สร้างผู้ใช้ (ผู้รับหิ้ว) โดย email และเบอร์โทรไม่ซ้ำกันในแต่ละครั้ง */
    private User persistUser() {
        int n = SEQ.incrementAndGet();
        User user = new User();
        user.setEmail("runner" + n + "@example.com");
        user.setPasswordHash("hash");
        user.setFullName("Runner " + n);
        user.setPhoneNumber(String.format("08%08d", n)); // 10 หลัก
        return em.persistAndFlush(user);
    }

    private DeliveryRound persistRound(User runner, String storeName, LocalDateTime scheduledAt,
            String status) {
        DeliveryRound round = new DeliveryRound();
        round.setRunner(runner);
        round.setStoreName(storeName);
        round.setLocationName("หน้าอาคาร SC1");
        round.setScheduledAt(scheduledAt);
        round.setServiceFee(new BigDecimal("20.00"));
        round.setStatus(status);
        return em.persistAndFlush(round);
    }
}