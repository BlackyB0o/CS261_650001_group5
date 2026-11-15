package com.example.demo.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Task56 (US10) บัญชีแอดมินไม่มีเบอร์โทร จึงต้องให้ users.phone_number เป็น NULL ได้
 * ddl-auto=update เพิ่ม column ใหม่ได้แต่ไม่ถอด NOT NULL ของ column เดิม จึงสั่งเองตอนเริ่มแอป (สั่งซ้ำได้ไม่มีผล)
 * ทำก่อน AdminAccountInitializer
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class UserPhoneNumberNullableMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN phone_number DROP NOT NULL");
        } catch (RuntimeException ex) {
            log.warn("Could not make users.phone_number nullable: {}", ex.getMessage());
        }
    }
}
