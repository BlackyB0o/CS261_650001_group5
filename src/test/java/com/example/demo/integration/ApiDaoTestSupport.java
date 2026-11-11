package com.example.demo.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtAuthFilter;
import com.example.demo.security.JwtService;
import com.jayway.jsonpath.JsonPath;

/**
 * ตัวช่วยร่วมของ Integration Test ระหว่าง API กับ DAO
 * ยิง request ผ่าน MockMvc (Controller -> Service -> Repository -> ฐานข้อมูล H2)
 * แล้วตรวจข้อมูลในฐานข้อมูลจริงด้วย JdbcTemplate
 */
@SpringBootTest
@TestPropertySource(properties = "app.upload-dir=target/test-uploads")
abstract class ApiDaoTestSupport {

    protected static final String AUTH = "Authorization";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Autowired private WebApplicationContext context;
    @Autowired private JwtAuthFilter jwtAuthFilter;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired protected JdbcTemplate jdbc;

    protected MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(jwtAuthFilter).build();
    }

    /** ล้างข้อมูลหลังแต่ละ test ให้ทุกข้อเริ่มจากฐานข้อมูลว่าง (ลบตามลำดับ foreign key) */
    @AfterEach
    void cleanDatabase() {
        for (String table : List.of("order_completions", "payments", "order_items", "orders",
                "posts", "delivery_rounds", "otp_verifications", "users")) {
            jdbc.update("DELETE FROM " + table);
        }
    }

    // ---------- ผู้ใช้และ token ----------

    protected User createUser(String fullName) {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(unique + "@test.com");
        user.setPhoneNumber("08" + String.format("%08d", Math.abs(unique.hashCode()) % 100_000_000));
        user.setPasswordHash("not-used-in-this-test");
        return userRepository.save(user);
    }

    protected String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user.getId());
    }

    // ---------- อ่านค่าและนับแถว ----------

    protected long count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    protected static String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    protected static Long readId(MvcResult result, String path) throws Exception {
        return ((Number) JsonPath.read(body(result), path)).longValue();
    }

    protected static String tomorrowAt(int hour) {
        return LocalDateTime.now().plusDays(1).withHour(hour).withMinute(0).withSecond(0).withNano(0).format(DATE_TIME);
    }

    protected static String yesterdayAt(int hour) {
        return LocalDateTime.now().minusDays(1).withHour(hour).withMinute(0).withSecond(0).withNano(0).format(DATE_TIME);
    }

    protected static String tomorrowDate() {
        return LocalDate.now().plusDays(1).toString();
    }

    // ---------- สร้างข้อมูลตั้งต้นผ่าน API ----------

    protected Long createRound(User runner, String storeName, String scheduledAt, String serviceFee) throws Exception {
        String json = """
                {"storeName":"%s","locationName":"อาคาร SC1 มธ.รังสิต","scheduledAt":"%s","serviceFee":%s}
                """.formatted(storeName, scheduledAt, serviceFee);
        MvcResult result = mockMvc.perform(post("/api/v1/delivery-rounds").header(AUTH, bearer(runner))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn();
        return readId(result, "$.data.roundId");
    }

    protected void changeRoundStatus(User runner, Long roundId, String newStatus) throws Exception {
        mockMvc.perform(patch("/api/v1/delivery-rounds/{id}/status", roundId).header(AUTH, bearer(runner))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + newStatus + "\"}"))
                .andExpect(status().isOk());
    }

    protected Long createOrder(User buyer, Long roundId) throws Exception {
        String json = """
                {"roundId":%d,"handoffMethod":"MEETUP","handoffLocation":"หน้าตึก SC1",
                 "items":[{"productName":"ลาเต้เย็น","quantity":2,"note":"หวานน้อย"}]}
                """.formatted(roundId);
        MvcResult result = mockMvc.perform(post("/api/v1/orders").header(AUTH, bearer(buyer))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn();
        return readId(result, "$.data.orderId");
    }

    protected void setPrice(User runner, Long orderId, String productAmount) throws Exception {
        mockMvc.perform(put("/api/v1/orders/{id}/price", orderId).header(AUTH, bearer(runner))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productAmount\":" + productAmount + "}"))
                .andExpect(status().isOk());
    }

    protected void paySlip(User buyer, Long orderId, String paidAmount) throws Exception {
        mockMvc.perform(multipart("/api/v1/orders/{id}/payment/slip", orderId)
                        .file(pngFile("slipImage", "slip.png"))
                        .param("paidAmount", paidAmount)
                        .header(AUTH, bearer(buyer)))
                .andExpect(status().isCreated());
    }

    protected static MockMultipartFile pngFile(String field, String fileName) {
        return new MockMultipartFile(field, fileName, "image/png", new byte[] {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3});
    }

    protected static MockMultipartFile jpgFile(String field, String fileName) {
        return new MockMultipartFile(field, fileName, "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, 1, 2, 3});
    }

    protected static MockMultipartFile pdfFile(String field, String fileName) {
        return new MockMultipartFile(field, fileName, "application/pdf", "%PDF-1.4 test".getBytes(StandardCharsets.UTF_8));
    }
}
