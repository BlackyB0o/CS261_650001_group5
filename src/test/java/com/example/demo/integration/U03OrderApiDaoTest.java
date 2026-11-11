package com.example.demo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.example.demo.entity.User;
import com.jayway.jsonpath.JsonPath;

/** Task18:U03 ฝากซื้อสินค้า/อาหาร : Integration Test ระหว่าง API กับ DAO (ตาราง orders, order_items) */
class U03OrderApiDaoTest extends ApiDaoTestSupport {

    private String orderJson(Long roundId) {
        return """
                {"roundId":%d,"handoffMethod":"MEETUP","handoffLocation":"หน้าตึก SC1",
                 "items":[{"productName":"ลาเต้เย็น","quantity":2,"note":"หวานน้อย"},
                          {"productName":"ครัวซองต์","quantity":1}]}
                """.formatted(roundId);
    }

    @Test
    @DisplayName("IT-U03-AD-01 สร้างออเดอร์และรายการสินค้าในฐานข้อมูล")
    void IT_U03_AD_01_createOrderSavesOrderAndItems() throws Exception {
        User runnerA = createUser("Runner A");
        User buyerB = createUser("Buyer B");
        Long roundR = createRound(runnerA, "ร้านกาแฟหน้าตึก SC", tomorrowAt(17), "10");

        MvcResult result = mockMvc.perform(post("/api/v1/orders").header(AUTH, bearer(buyerB))
                        .contentType(MediaType.APPLICATION_JSON).content(orderJson(roundR)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();
        Long orderId = readId(result, "$.data.orderId");

        Map<String, Object> order = jdbc.queryForMap("SELECT * FROM orders WHERE id = ?", orderId);
        assertThat(((Number) order.get("BUYER_ID")).longValue()).isEqualTo(buyerB.getId());
        assertThat(((Number) order.get("ROUND_ID")).longValue()).isEqualTo(roundR);
        assertThat(order.get("HANDOFF_METHOD")).isEqualTo("MEETUP");
        assertThat(order.get("HANDOFF_LOCATION")).isEqualTo("หน้าตึก SC1");
        assertThat(order.get("STATUS")).isEqualTo("PENDING");

        List<Map<String, Object>> items = jdbc.queryForList(
                "SELECT product_name, quantity, note FROM order_items WHERE order_id = ? ORDER BY id", orderId);
        assertThat(items).hasSize(2);
        assertThat(items.get(0).get("PRODUCT_NAME")).isEqualTo("ลาเต้เย็น");
        assertThat(((Number) items.get(0).get("QUANTITY")).intValue()).isEqualTo(2);
        assertThat(items.get(0).get("NOTE")).isEqualTo("หวานน้อย");
        assertThat(items.get(1).get("PRODUCT_NAME")).isEqualTo("ครัวซองต์");
        assertThat(((Number) items.get(1).get("QUANTITY")).intValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("IT-U03-AD-02 ไม่ส่งจุดนัดรับและไม่มีสินค้า ต้องไม่บันทึก")
    void IT_U03_AD_02_missingFieldsNotSaved() throws Exception {
        User runnerA = createUser("Runner A");
        User buyerB = createUser("Buyer B");
        Long roundR = createRound(runnerA, "ร้านกาแฟหน้าตึก SC", tomorrowAt(17), "10");
        long ordersBefore = count("orders");
        long itemsBefore = count("order_items");

        mockMvc.perform(post("/api/v1/orders").header(AUTH, bearer(buyerB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roundId\":" + roundR + ",\"handoffMethod\":\"MEETUP\",\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'handoffLocation')].message").value("must not be blank"))
                .andExpect(jsonPath("$.errors[?(@.field == 'items')].message").value("must not be empty"));

        assertThat(count("orders")).isEqualTo(ordersBefore);
        assertThat(count("order_items")).isEqualTo(itemsBefore);
    }

    @Test
    @DisplayName("IT-U03-AD-03 สั่งของในรอบของตัวเองไม่ได้")
    void IT_U03_AD_03_cannotOrderFromOwnRound() throws Exception {
        User runnerA = createUser("Runner A");
        Long roundR = createRound(runnerA, "ร้านกาแฟหน้าตึก SC", tomorrowAt(17), "10");
        long before = count("orders");

        mockMvc.perform(post("/api/v1/orders").header(AUTH, bearer(runnerA))
                        .contentType(MediaType.APPLICATION_JSON).content(orderJson(roundR)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You cannot order from your own round"));

        assertThat(count("orders")).isEqualTo(before);
    }

    @Test
    @DisplayName("IT-U03-AD-04 สั่งของในรอบที่ปิดแล้วไม่ได้")
    void IT_U03_AD_04_cannotOrderFromClosedRound() throws Exception {
        User runnerA = createUser("Runner A");
        User buyerB = createUser("Buyer B");
        Long closedRound = createRound(runnerA, "ร้านกาแฟหน้าตึก SC", tomorrowAt(17), "10");
        changeRoundStatus(runnerA, closedRound, "CLOSED");
        long before = count("orders");

        mockMvc.perform(post("/api/v1/orders").header(AUTH, bearer(buyerB))
                        .contentType(MediaType.APPLICATION_JSON).content(orderJson(closedRound)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Delivery round is not open"));

        assertThat(count("orders")).isEqualTo(before);
    }

    @Test
    @DisplayName("IT-U03-AD-05 ผู้รับหิ้วดึงออเดอร์ในรอบตรงกับฐานข้อมูล")
    void IT_U03_AD_05_runnerOrdersInRoundMatchDatabase() throws Exception {
        User runnerA = createUser("Runner A");
        User buyerB = createUser("Buyer B");
        Long roundR = createRound(runnerA, "ร้านกาแฟหน้าตึก SC", tomorrowAt(17), "10");
        createOrder(buyerB, roundR);
        Thread.sleep(20);
        createOrder(buyerB, roundR);

        MvcResult result = mockMvc.perform(get("/api/v1/orders").param("roundId", roundR.toString())
                        .header(AUTH, bearer(runnerA)))
                .andExpect(status().isOk())
                .andReturn();

        String json = body(result);
        List<Number> apiIds = JsonPath.read(json, "$.data[*].orderId");
        List<Long> dbIds = jdbc.queryForList(
                "SELECT id FROM orders WHERE round_id = ? ORDER BY created_at", Long.class, roundR);
        assertThat(apiIds.stream().map(Number::longValue).toList()).isEqualTo(dbIds).hasSize(2);
        for (int i = 0; i < dbIds.size(); i++) {
            List<?> apiItems = JsonPath.read(json, "$.data[" + i + "].items");
            long dbItems = jdbc.queryForObject("SELECT COUNT(*) FROM order_items WHERE order_id = ?", Long.class, dbIds.get(i));
            assertThat((long) apiItems.size()).isEqualTo(dbItems);
        }
    }
}
