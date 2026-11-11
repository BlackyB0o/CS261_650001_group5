package com.example.demo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.example.demo.entity.User;
import com.jayway.jsonpath.JsonPath;

/** Task10:U01 เปิดรอบรับหิ้ว : Integration Test ระหว่าง API กับ DAO (ตาราง delivery_rounds) */
class U01DeliveryRoundApiDaoTest extends ApiDaoTestSupport {

    @Test
    @DisplayName("IT-U01-AD-01 สร้างรอบและบันทึกลงฐานข้อมูล")
    void IT_U01_AD_01_createRoundSavesRow() throws Exception {
        User runnerA = createUser("Runner A");
        String scheduledAt = tomorrowAt(17);
        String json = """
                {"storeName":"ร้านกาแฟหน้าตึก SC","locationName":"อาคาร SC1 มธ.รังสิต","scheduledAt":"%s","serviceFee":10}
                """.formatted(scheduledAt);

        MvcResult result = mockMvc.perform(post("/api/v1/delivery-rounds").header(AUTH, bearer(runnerA))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andReturn();
        Long roundId = readId(result, "$.data.roundId");

        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM delivery_rounds WHERE id = ?", roundId);
        assertThat(row.get("STORE_NAME")).isEqualTo("ร้านกาแฟหน้าตึก SC");
        assertThat(row.get("LOCATION_NAME")).isEqualTo("อาคาร SC1 มธ.รังสิต");
        assertThat(((Timestamp) row.get("SCHEDULED_AT")).toLocalDateTime().toString()).startsWith(scheduledAt.substring(0, 16));
        assertThat((BigDecimal) row.get("SERVICE_FEE")).isEqualByComparingTo("10.00");
        assertThat(row.get("STATUS")).isEqualTo("OPEN");
        assertThat(((Number) row.get("RUNNER_ID")).longValue()).isEqualTo(runnerA.getId());
    }

    @Test
    @DisplayName("IT-U01-AD-02 ข้อมูลไม่ครบ ต้องไม่บันทึก")
    void IT_U01_AD_02_missingFieldsNotSaved() throws Exception {
        User runnerA = createUser("Runner A");
        long before = count("delivery_rounds");
        String json = """
                {"locationName":"อาคาร SC1","scheduledAt":"%s"}
                """.formatted(tomorrowAt(17));

        mockMvc.perform(post("/api/v1/delivery-rounds").header(AUTH, bearer(runnerA))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'storeName')].message").value("must not be blank"))
                .andExpect(jsonPath("$.errors[?(@.field == 'serviceFee')].message").value("must not be null"));

        assertThat(count("delivery_rounds")).isEqualTo(before);
    }

    @Test
    @DisplayName("IT-U01-AD-03 วันเวลาเป็นอดีต ต้องไม่บันทึก")
    void IT_U01_AD_03_pastScheduledAtNotSaved() throws Exception {
        User runnerA = createUser("Runner A");
        long before = count("delivery_rounds");
        String json = """
                {"storeName":"ร้านกาแฟหน้าตึก SC","locationName":"อาคาร SC1 มธ.รังสิต","scheduledAt":"%s","serviceFee":10}
                """.formatted(yesterdayAt(17));

        mockMvc.perform(post("/api/v1/delivery-rounds").header(AUTH, bearer(runnerA))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'scheduledAt')].message").value("must be a future date"));

        assertThat(count("delivery_rounds")).isEqualTo(before);
    }

    @Test
    @DisplayName("IT-U01-AD-04 ดึงรายการรอบที่เปิดอยู่ตรงกับฐานข้อมูล")
    void IT_U01_AD_04_openRoundsMatchDatabase() throws Exception {
        User runnerA = createUser("Runner A");
        createRound(runnerA, "Round later", tomorrowAt(18), "10");
        createRound(runnerA, "Round earlier", tomorrowAt(9), "10");
        Long closed = createRound(runnerA, "Round closed", tomorrowAt(12), "10");
        changeRoundStatus(runnerA, closed, "CLOSED");

        MvcResult result = mockMvc.perform(get("/api/v1/delivery-rounds"))
                .andExpect(status().isOk())
                .andReturn();

        List<Number> apiIds = JsonPath.read(body(result), "$.data[*].roundId");
        List<Long> dbIds = jdbc.queryForList(
                "SELECT id FROM delivery_rounds WHERE status = 'OPEN' ORDER BY scheduled_at", Long.class);
        assertThat(apiIds.stream().map(Number::longValue).toList()).isEqualTo(dbIds).hasSize(2);
    }

    @Test
    @DisplayName("IT-U01-AD-05 ดูรายละเอียดรอบตรงกับฐานข้อมูล")
    void IT_U01_AD_05_roundDetailMatchesDatabase() throws Exception {
        User runnerA = createUser("Runner A");
        Long roundId = createRound(runnerA, "ร้านกาแฟหน้าตึก SC", tomorrowAt(17), "10");

        MvcResult result = mockMvc.perform(get("/api/v1/delivery-rounds/{id}", roundId))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM delivery_rounds WHERE id = ?", roundId);
        String json = body(result);
        assertThat((String) JsonPath.read(json, "$.data.storeName")).isEqualTo(row.get("STORE_NAME"));
        assertThat((String) JsonPath.read(json, "$.data.locationName")).isEqualTo(row.get("LOCATION_NAME"));
        assertThat((String) JsonPath.read(json, "$.data.scheduledAt"))
                .startsWith(((Timestamp) row.get("SCHEDULED_AT")).toLocalDateTime().toString().substring(0, 16));
        assertThat(new BigDecimal(JsonPath.read(json, "$.data.serviceFee").toString()))
                .isEqualByComparingTo((BigDecimal) row.get("SERVICE_FEE"));
        assertThat((String) JsonPath.read(json, "$.data.status")).isEqualTo(row.get("STATUS"));
        assertThat(((Number) JsonPath.read(json, "$.data.runnerId")).longValue())
                .isEqualTo(((Number) row.get("RUNNER_ID")).longValue());
    }
}
