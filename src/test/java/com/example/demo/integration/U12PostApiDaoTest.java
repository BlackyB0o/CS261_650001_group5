package com.example.demo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.example.demo.entity.User;
import com.jayway.jsonpath.JsonPath;

/** Task14:U12 ผู้ฝากหิ้วโพสต์หาคนรับหิ้ว : Integration Test ระหว่าง API กับ DAO (ตาราง posts) */
class U12PostApiDaoTest extends ApiDaoTestSupport {

    private String postJson(String title) {
        return """
                {"postTitle":"%s","shopName":"ร้านชานม ตึก SC","productDetail":"ชานมไข่มุก หวานน้อย",
                 "quantity":2,"carryFee":20,"desiredDate":"%s","location":"หน้าหอพักเชียงราก 1","budget":100}
                """.formatted(title, tomorrowDate());
    }

    private Long createPost(User buyer, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/posts").header(AUTH, bearer(buyer))
                        .contentType(MediaType.APPLICATION_JSON).content(postJson(title)))
                .andExpect(status().isCreated()).andReturn();
        return readId(result, "$.data.postId");
    }

    @Test
    @DisplayName("IT-U12-AD-01 สร้างโพสต์และบันทึกลงฐานข้อมูล")
    void IT_U12_AD_01_createPostSavesRow() throws Exception {
        User buyerB = createUser("Buyer B");

        MvcResult result = mockMvc.perform(post("/api/v1/posts").header(AUTH, bearer(buyerB))
                        .contentType(MediaType.APPLICATION_JSON).content(postJson("ฝากซื้อชานมไข่มุก")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andReturn();
        Long postId = readId(result, "$.data.postId");

        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM posts WHERE id = ?", postId);
        assertThat(row.get("POST_TITLE")).isEqualTo("ฝากซื้อชานมไข่มุก");
        assertThat(row.get("SHOP_NAME")).isEqualTo("ร้านชานม ตึก SC");
        assertThat(row.get("PRODUCT_DETAIL")).isEqualTo("ชานมไข่มุก หวานน้อย");
        assertThat(((Number) row.get("QUANTITY")).intValue()).isEqualTo(2);
        assertThat((BigDecimal) row.get("CARRY_FEE")).isEqualByComparingTo("20.00");
        assertThat(((Date) row.get("DESIRED_DATE")).toLocalDate().toString()).isEqualTo(tomorrowDate());
        assertThat(row.get("LOCATION")).isEqualTo("หน้าหอพักเชียงราก 1");
        assertThat((BigDecimal) row.get("BUDGET")).isEqualByComparingTo("100.00");
        assertThat(row.get("STATUS")).isEqualTo("OPEN");
        assertThat(((Number) row.get("BUYER_ID")).longValue()).isEqualTo(buyerB.getId());
        assertThat(row.get("ACCEPTED_BY")).isNull();
        assertThat(row.get("CREATED_AT")).isNotNull();
    }

    @Test
    @DisplayName("IT-U12-AD-02 ไม่ส่งรายละเอียดสินค้าและสถานที่ ต้องไม่บันทึก")
    void IT_U12_AD_02_missingFieldsNotSaved() throws Exception {
        User buyerB = createUser("Buyer B");
        long before = count("posts");
        String json = """
                {"postTitle":"ฝากซื้อชานมไข่มุก","shopName":"ร้านชานม ตึก SC","quantity":2,"carryFee":20,
                 "desiredDate":"%s","budget":100}
                """.formatted(tomorrowDate());

        mockMvc.perform(post("/api/v1/posts").header(AUTH, bearer(buyerB))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'productDetail')].message").value("must not be blank"))
                .andExpect(jsonPath("$.errors[?(@.field == 'location')].message").value("must not be blank"));

        assertThat(count("posts")).isEqualTo(before);
    }

    @Test
    @DisplayName("IT-U12-AD-03 ดึงรายการโพสต์ที่เปิดอยู่ตรงกับฐานข้อมูล")
    void IT_U12_AD_03_openPostsMatchDatabase() throws Exception {
        User runnerA = createUser("Runner A");
        User buyerB = createUser("Buyer B");
        createPost(buyerB, "Post 1");
        Thread.sleep(20);
        Long accepted = createPost(buyerB, "Post 2 (will be accepted)");
        Thread.sleep(20);
        createPost(buyerB, "Post 3");
        mockMvc.perform(patch("/api/v1/posts/{id}/accept", accepted).header(AUTH, bearer(runnerA)))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/v1/posts"))
                .andExpect(status().isOk())
                .andReturn();

        List<Number> apiIds = JsonPath.read(body(result), "$.data[*].postId");
        List<Long> dbIds = jdbc.queryForList(
                "SELECT id FROM posts WHERE status = 'OPEN' ORDER BY created_at DESC", Long.class);
        assertThat(apiIds.stream().map(Number::longValue).toList()).isEqualTo(dbIds).hasSize(2);
    }

    @Test
    @DisplayName("IT-U12-AD-04 ผู้ใช้อื่นรับงาน")
    void IT_U12_AD_04_otherUserAcceptsPost() throws Exception {
        User runnerA = createUser("Runner A");
        User buyerB = createUser("Buyer B");
        Long postId = createPost(buyerB, "ฝากซื้อชานมไข่มุก");

        mockMvc.perform(patch("/api/v1/posts/{id}/accept", postId).header(AUTH, bearer(runnerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.acceptedBy").value(runnerA.getId().intValue()));

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT status, accepted_by, accepted_at FROM posts WHERE id = ?", postId);
        assertThat(row.get("STATUS")).isEqualTo("ACCEPTED");
        assertThat(((Number) row.get("ACCEPTED_BY")).longValue()).isEqualTo(runnerA.getId());
        assertThat(row.get("ACCEPTED_AT")).isNotNull();
    }

    @Test
    @DisplayName("IT-U12-AD-05 รับโพสต์ของตัวเองไม่ได้")
    void IT_U12_AD_05_cannotAcceptOwnPost() throws Exception {
        User buyerB = createUser("Buyer B");
        Long postId = createPost(buyerB, "ฝากซื้อชานมไข่มุก");

        mockMvc.perform(patch("/api/v1/posts/{id}/accept", postId).header(AUTH, bearer(buyerB)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You cannot accept your own post"));

        Map<String, Object> row = jdbc.queryForMap("SELECT status, accepted_by FROM posts WHERE id = ?", postId);
        assertThat(row.get("STATUS")).isEqualTo("OPEN");
        assertThat(row.get("ACCEPTED_BY")).isNull();
    }
}
