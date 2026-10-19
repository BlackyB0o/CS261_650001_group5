package com.example.demo.service;

/** ส่ง SMS (Sprint 1 ใช้ MockSmsSender พิมพ์ลง log อย่างเดียว) */
public interface SmsSender {

    void send(String phoneNumber, String message);
}
