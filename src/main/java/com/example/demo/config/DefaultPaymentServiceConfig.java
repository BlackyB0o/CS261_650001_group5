package com.example.demo.config;

import com.example.demo.service.PaymentService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * PaymentService ชั่วคราว (ตอบ 501) ให้แอปรันได้ก่อน เมื่อมี @Service จริง เช่น PaymentServiceImpl ตัวนี้จะถูกข้ามเอง
 * ลบไฟล์นี้ได้เมื่อ Service จริงเสร็จแล้ว
 */
@Configuration
public class DefaultPaymentServiceConfig {

    @Bean
    @ConditionalOnMissingBean(PaymentService.class)
    PaymentService defaultPaymentService() {
        return NotImplementedService.of(PaymentService.class);
    }
}
