package com.example.demo.repository;

import com.example.demo.entity.Post;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    /** โพสต์ตามสถานะ ใหม่สุดก่อน (GET /posts ใช้ OPEN) */
    List<Post> findByStatusOrderByCreatedAtDesc(String status);

    /** โพสต์ของฉัน ทุกสถานะ (GET /posts/my) */
    List<Post> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);
}
