package com.example.demo.service;

import com.example.demo.dto.CreatePostRequestDTO;
import com.example.demo.dto.PostResponseDTO;
import java.util.List;

/** Task12 */
public interface PostService {

    PostResponseDTO createPost(Long buyerId, CreatePostRequestDTO request);

    List<PostResponseDTO> getOpenPosts();

    List<PostResponseDTO> getMyPosts(Long buyerId);

    PostResponseDTO getPost(Long postId);

    /** 403 รับโพสต์ตัวเอง, 409 โพสต์ไม่ใช่ OPEN */
    PostResponseDTO acceptPost(Long userId, Long postId);
}
