package com.example.demo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.example.demo.entity.User;
import com.jayway.jsonpath.JsonPath;

/** Task26:U08 ยืนยันการได้รับสินค้าและปิดออเดอร์ : Integration Test ระหว่าง API กับ DAO (ตาราง order_completions, payments, orders) */
class U08OrderCompletionApiDaoTest extends ApiDaoTestSupport {

    private User runnerA;
    private User buyerB;
    private Long order1;

    /** ออเดอร์ O1 ของผู้ใช้ B ชำระแล้ว: orders.status = PAID, payments.payment_status = HELD, ยอดรวม 130 บาท */
    private void preparePaidOrder() throws Exception {
        runnerA = createUser("Runner A");
        buyerB = createUser("Buyer B");
        Long round = createRound(runnerA, "ร้านกาแฟหน้าตึก SC", tomorrowAt(17), "10");
        order1 = createOrder(buyerB, round);
        setPrice(runnerA, order1, "120");
        paySlip(buyerB, order1, "130");
    }

    private Map<String, Object> paymentAndOrder() {
        return jdbc.queryForMap("SELECT p.payment_status, o.status FROM payments p JOIN orders o ON o.id = p.order_id WHERE o.id = ?", order1);
    }

    @Test
    @DisplayName("IT-U08-AD-01 ไม่แนบรูป ต้องไม่ปิดออเดอร์")
    void IT_U08_AD_01_noImageNotCompleted() throws Exception {
        preparePaidOrder();

        mockMvc.perform(multipart("/api/v1/orders/{id}/completion", order1).header(AUTH, bearer(buyerB)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'evidenceImage')].message").value("must not be empty"));

        assertThat(count("order_completions")).isZero();
        Map<String, Object> row = paymentAndOrder();
        assertThat(row.get("PAYMENT_STATUS")).isEqualTo("HELD");
        assertThat(row.get("STATUS")).isEqualTo("PAID");
    }

    @Test
    @DisplayName("IT-U08-AD-02 แนบไฟล์ที่ไม่ใช่รูป ต้องไม่ปิดออเดอร์")
    void IT_U08_AD_02_pdfNotCompleted() throws Exception {
        preparePaidOrder();

        mockMvc.perform(multipart("/api/v1/orders/{id}/completion", order1)
                        .file(pdfFile("evidenceImage", "test.pdf")).header(AUTH, bearer(buyerB)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'evidenceImage')].message")
                        .value("only JPEG or PNG images are allowed"));

        assertThat(count("order_completions")).isZero();
        Map<String, Object> row = paymentAndOrder();
        assertThat(row.get("PAYMENT_STATUS")).isEqualTo("HELD");
        assertThat(row.get("STATUS")).isEqualTo("PAID");
    }

    @Test
    @DisplayName("IT-U08-AD-03 ยืนยันรับสินค้าสำเร็จ ปล่อยเงินและปิดออเดอร์")
    void IT_U08_AD_03_completionReleasesPaymentAndClosesOrder() throws Exception {
        preparePaidOrder();

        MvcResult result = mockMvc.perform(multipart("/api/v1/orders/{id}/completion", order1)
                        .file(jpgFile("evidenceImage", "proof.jpg")).header(AUTH, bearer(buyerB)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.releasedAmount").value(130.0))
                .andReturn();
        Long paymentId = readId(result, "$.data.paymentId");

        Map<String, Object> completion = jdbc.queryForMap("SELECT * FROM order_completions WHERE payment_id = ?", paymentId);
        assertThat(((Number) completion.get("CONFIRMED_BY")).longValue()).isEqualTo(buyerB.getId());
        assertThat((String) completion.get("EVIDENCE_IMAGE_URL")).startsWith("/uploads/completions/");
        assertThat(completion.get("COMPLETION_STATUS")).isEqualTo("COMPLETED");
        assertThat(completion.get("CONFIRMED_AT")).isNotNull();
        assertThat(jdbc.queryForObject("SELECT payment_status FROM payments WHERE id = ?", String.class, paymentId))
                .isEqualTo("RELEASED");
        assertThat(jdbc.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, order1))
                .isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("IT-U08-AD-04 ยืนยันซ้ำไม่ได้")
    void IT_U08_AD_04_cannotCompleteTwice() throws Exception {
        preparePaidOrder();
        mockMvc.perform(multipart("/api/v1/orders/{id}/completion", order1)
                        .file(jpgFile("evidenceImage", "proof.jpg")).header(AUTH, bearer(buyerB)))
                .andExpect(status().isCreated());

        mockMvc.perform(multipart("/api/v1/orders/{id}/completion", order1)
                        .file(jpgFile("evidenceImage", "proof.jpg")).header(AUTH, bearer(buyerB)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Order is already completed"));

        long rows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM order_completions c JOIN payments p ON p.id = c.payment_id WHERE p.order_id = ?",
                Long.class, order1);
        assertThat(rows).isEqualTo(1);
    }

    @Test
    @DisplayName("IT-U08-AD-05 ผู้รับหิ้วดูผลการยืนยันตรงกับฐานข้อมูล")
    void IT_U08_AD_05_runnerViewsCompletionMatchingDatabase() throws Exception {
        preparePaidOrder();
        mockMvc.perform(multipart("/api/v1/orders/{id}/completion", order1)
                        .file(jpgFile("evidenceImage", "proof.jpg")).header(AUTH, bearer(buyerB)))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/api/v1/orders/{id}/completion", order1).header(AUTH, bearer(runnerA)))
                .andExpect(status().isOk())
                .andReturn();

        String json = body(result);
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT c.*, p.total_amount FROM order_completions c JOIN payments p ON p.id = c.payment_id WHERE p.order_id = ?",
                order1);
        assertThat(((Number) JsonPath.read(json, "$.data.completionId")).longValue()).isEqualTo(((Number) row.get("ID")).longValue());
        assertThat(((Number) JsonPath.read(json, "$.data.confirmedBy")).longValue()).isEqualTo(((Number) row.get("CONFIRMED_BY")).longValue());
        assertThat((String) JsonPath.read(json, "$.data.evidenceImageUrl")).isEqualTo(row.get("EVIDENCE_IMAGE_URL"));
        assertThat((String) JsonPath.read(json, "$.data.completionStatus")).isEqualTo(row.get("COMPLETION_STATUS"));
        assertThat(new BigDecimal(JsonPath.read(json, "$.data.releasedAmount").toString()))
                .isEqualByComparingTo((BigDecimal) row.get("TOTAL_AMOUNT"));
    }
}
