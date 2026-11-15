package com.example.demo.entity;

import com.example.demo.common.UserRoles;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ตาราง users (Task04) ผู้ใช้ทั่วไป (role USER) ทำได้ทั้งฝากหิ้วและรับหิ้วในบัญชีเดียว
 * Task54 (US10) เพิ่ม role ADMIN สำหรับผู้ดูแลระบบ และ suspended สำหรับบัญชีที่ถูกระงับ
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    /** ผู้ใช้ทั่วไปต้องมีเสมอ (บังคับตอนสมัคร) บัญชีแอดมินไม่ต้องมี */
    @Column(name = "phone_number", unique = true, length = 10)
    private String phoneNumber;

    /** USER หรือ ADMIN (ดู UserRoles) */
    @Column(name = "role", nullable = false, length = 20, columnDefinition = "varchar(20) default 'USER'")
    private String role = UserRoles.USER;

    /** true = ถูกแอดมินระงับ เข้าสู่ระบบและใช้ token เดิมไม่ได้จนกว่าจะคืนสิทธิ์ */
    @Column(name = "suspended", nullable = false, columnDefinition = "boolean default false")
    private boolean suspended;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (role == null) {
            role = UserRoles.USER;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
