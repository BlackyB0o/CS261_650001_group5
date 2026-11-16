package com.example.demo.config;

import com.example.demo.common.UserRoles;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.PasswordHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Task56 (US10) สร้างบัญชีแอดมินตอนเริ่มแอป จาก APP_ADMIN_EMAIL และ APP_ADMIN_PASSWORD
 * ไม่ได้ตั้งค่า = ไม่มีบัญชีแอดมิน, มีบัญชีอีเมลนี้อยู่แล้ว = ตั้งเป็นแอดมินและใช้รหัสผ่านตามค่าที่ตั้ง
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    @Value("${app.admin.email:}")
    private String email;

    @Value("${app.admin.password:}")
    private String password;

    @Value("${app.admin.name:CarryHub Admin}")
    private String fullName;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            if (!email.isBlank() || !password.isBlank()) {
                log.warn("Set both APP_ADMIN_EMAIL and APP_ADMIN_PASSWORD to create the admin account");
            }
            return;
        }
        String normalizedEmail = email.trim().toLowerCase();
        User admin = userRepository.findByEmail(normalizedEmail).orElseGet(() -> {
            User user = new User();
            user.setEmail(normalizedEmail);
            user.setFullName(fullName.isBlank() ? "CarryHub Admin" : fullName.trim());
            return user;
        });
        if (admin.getPasswordHash() == null || !passwordHasher.matches(password, admin.getPasswordHash())) {
            admin.setPasswordHash(passwordHasher.hash(password));
        }
        admin.setRole(UserRoles.ADMIN);
        admin.setSuspended(false);
        userRepository.save(admin);
        log.info("Admin account ready: {}", normalizedEmail);
    }
}
