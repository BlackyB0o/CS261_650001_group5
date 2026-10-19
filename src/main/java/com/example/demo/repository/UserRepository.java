package com.example.demo.repository;

import com.example.demo.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    /** ค้นหาตอน Login (ส่ง identifier ทั้งสองช่อง) */
    Optional<User> findByEmailOrPhoneNumber(String email, String phoneNumber);

    /** ค้นหาตอนลืมรหัสผ่าน */
    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);
}
