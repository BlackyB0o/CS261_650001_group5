package com.example.demo.service.impl;

import com.example.demo.service.SmsSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Sprint 1 ยังไม่ส่ง SMS จริง พิมพ์ข้อความ (รวม OTP) ลง log ของแอปแทน
 * ดู OTP ได้จาก terminal ที่รันแอป หรือ docker compose logs app
 */
@Component
public class MockSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(MockSmsSender.class);

    @Override
    public void send(String phoneNumber, String message) {
        log.info("[MOCK SMS] to {}: {}", phoneNumber, message);
    }
}
