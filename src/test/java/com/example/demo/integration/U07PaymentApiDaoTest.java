package com.example.demo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.example.demo.entity.User;
import com.jayway.jsonpath.JsonPath;

/** Task22:U07 ชำระค่าสินค้าและค่าหิ้วผ่านระบบตัวกลาง : Integration Test ระหว่าง API กับ DAO (ตาราง payments, orders) */
class U07PaymentApiDaoTest extends ApiDaoTestSupport {

    private User runnerA;
    private User buyerB;
    private Long order1;

    /** รอบของผู้ใช้ A ค่าหิ้ว 10 บาท และออเดอร์ O1 ของผู้ใช้ B สถานะ PENDING */
    private void prepareOrder() throws Exception {
        runnerA = createUser("Runner A");
        buyerB = createUser("Buyer B");
        Long round = createRound(runnerA, "ร้านกาแฟหน้าตึก SC", tomorrowAt(17), "10");
        order1 = createOrder(buyerB, round);
    }

    private String orderStatus(Long orderId) {
        return jdbc.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, orderId);
    }

    @Test
    @DisplayName("IT-U07-AD-01 ผู้รับหิ้วกำหนดราคาและสร้างข้อมูลชำระเงิน")
    void IT_U07_AD_01_runnerSetsPriceCreatesPayment() throws Exception {
        prepareOrder();

        MvcResult result = mockMvc.perform(put("/api/v1/orders/{id}/price", order1).header(AUTH, bearer(runnerA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productAmount\":120}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(130.0))
                .andReturn();
        Long paymentId = readId(result, "$.data.paymentId");
        assertThat((String) JsonPath.read(body(result), "$.data.qrPayload")).isEqualTo("CARRYHUB-PAY|" + paymentId + "|130.00");

        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM payments WHERE order_id = ?", order1);
        assertThat((BigDecimal) row.get("PRODUCT_AMOUNT")).isEqualByComparingTo("120.00");
        assertThat((BigDecimal) row.get("SERVICE_FEE")).isEqualByComparingTo("10.00");
        assertThat((BigDecimal) row.get("TOTAL_AMOUNT")).isEqualByComparingTo("130.00");
        assertThat(row.get("PAYMENT_METHOD")).isEqualTo("QR_TRANSFER");
        assertThat(row.get("PAYMENT_STATUS")).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("IT-U07-AD-02 แนบสลิปก่อนผู้รับหิ้วกำหนดราคา")
    void IT_U07_AD_02_slipBeforePriceIsRejected() throws Exception {
        prepareOrder();

        mockMvc.perform(multipart("/api/v1/orders/{id}/payment/slip", order1)
                        .file(pngFile("slipImage", "slip.png")).param("paidAmount", "130")
                        .header(AUTH, bearer(buyerB)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Product price has not been set yet"));

        long payments = jdbc.queryForObject("SELECT COUNT(*) FROM payments WHERE order_id = ?", Long.class, order1);
        assertThat(payments).isZero();
        assertThat(orderStatus(order1)).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("IT-U07-AD-03 ยอดที่ชำระไม่ตรง ต้องไม่ยืนยันการชำระ")
    void IT_U07_AD_03_wrongAmountNotConfirmed() throws Exception {
        prepareOrder();
        setPrice(runnerA, order1, "120");

        mockMvc.perform(multipart("/api/v1/orders/{id}/payment/slip", order1)
                        .file(pngFile("slipImage", "slip.png")).param("paidAmount", "100")
                        .header(AUTH, bearer(buyerB)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'paidAmount')].message")
                        .value("must equal the total amount (130.00)"));

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT payment_status, slip_image_url FROM payments WHERE order_id = ?", order1);
        assertThat(row.get("PAYMENT_STATUS")).isEqualTo("PENDING");
        assertThat(row.get("SLIP_IMAGE_URL")).isNull();
        assertThat(orderStatus(order1)).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("IT-U07-AD-04 ชำระเงินสำเร็จ เงินพักไว้ที่ระบบกลาง")
    void IT_U07_AD_04_paymentHeldAndOrderPaid() throws Exception {
        prepareOrder();
        setPrice(runnerA, order1, "120");

        mockMvc.perform(multipart("/api/v1/orders/{id}/payment/slip", order1)
                        .file(pngFile("slipImage", "slip.png")).param("paidAmount", "130")
                        .header(AUTH, bearer(buyerB)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.paymentStatus").value("HELD"))
                .andExpect(jsonPath("$.data.orderStatus").value("PAID"));

        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM payments WHERE order_id = ?", order1);
        assertThat(row.get("PAYMENT_STATUS")).isEqualTo("HELD");
        assertThat((String) row.get("SLIP_IMAGE_URL")).startsWith("/uploads/slips/");
        assertThat((String) row.get("TRANSACTION_REFERENCE")).startsWith("TXN-");
        assertThat(row.get("PAID_AT")).isNotNull();
        assertThat(orderStatus(order1)).isEqualTo("PAID");
    }

    @Test
    @DisplayName("IT-U07-AD-05 ชำระซ้ำไม่ได้")
    void IT_U07_AD_05_cannotPayTwice() throws Exception {
        prepareOrder();
        setPrice(runnerA, order1, "120");
        paySlip(buyerB, order1, "130");
        Map<String, Object> before = jdbc.queryForMap(
                "SELECT transaction_reference, paid_at, slip_image_url FROM payments WHERE order_id = ?", order1);

        mockMvc.perform(multipart("/api/v1/orders/{id}/payment/slip", order1)
                        .file(pngFile("slipImage", "slip.png")).param("paidAmount", "130")
                        .header(AUTH, bearer(buyerB)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Order has already been paid"));

        Map<String, Object> after = jdbc.queryForMap(
                "SELECT transaction_reference, paid_at, slip_image_url FROM payments WHERE order_id = ?", order1);
        assertThat(after).isEqualTo(before);
    }
}
