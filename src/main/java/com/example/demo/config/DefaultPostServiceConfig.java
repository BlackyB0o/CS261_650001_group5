package com.example.demo.config;

import com.example.demo.service.PostService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * PostService ชั่วคราว (ตอบ 501) ให้แอปรันได้ก่อน เมื่อมี @Service จริง เช่น PostServiceImpl ตัวนี้จะถูกข้ามเอง
 * ลบไฟล์นี้ได้เมื่อ Service จริงเสร็จแล้ว
 */
@Configuration
public class DefaultPostServiceConfig {

    @Bean
    @ConditionalOnMissingBean(PostService.class)
    PostService defaultPostService() {
        return NotImplementedService.of(PostService.class);
    }
}
