package com.example.demo.config;

import com.example.demo.service.AuthService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AuthService ชั่วคราว (ตอบ 501) ให้แอปรันได้ก่อน เมื่อมี @Service จริง เช่น AuthServiceImpl ตัวนี้จะถูกข้ามเอง
 * ลบไฟล์นี้ได้เมื่อ Service จริงเสร็จแล้ว
 */
@Configuration
public class DefaultAuthServiceConfig {

    @Bean
    @ConditionalOnMissingBean(AuthService.class)
    AuthService defaultAuthService() {
        return NotImplementedService.of(AuthService.class);
    }
}
