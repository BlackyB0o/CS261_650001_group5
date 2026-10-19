package com.example.demo.config;

import com.example.demo.service.DeliveryRoundService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DeliveryRoundService ชั่วคราว (ตอบ 501) ให้แอปรันได้ก่อน เมื่อมี @Service จริง เช่น DeliveryRoundServiceImpl ตัวนี้จะถูกข้ามเอง
 * ลบไฟล์นี้ได้เมื่อ Service จริงเสร็จแล้ว
 */
@Configuration
public class DefaultDeliveryRoundServiceConfig {

    @Bean
    @ConditionalOnMissingBean(DeliveryRoundService.class)
    DeliveryRoundService defaultDeliveryRoundService() {
        return NotImplementedService.of(DeliveryRoundService.class);
    }
}
