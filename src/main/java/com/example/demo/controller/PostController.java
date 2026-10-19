package com.example.demo.controller;

import com.example.demo.common.RequestAttributes;
import com.example.demo.dto.ApiResponseDTO;
import com.example.demo.dto.CreatePostRequestDTO;
import com.example.demo.dto.PostResponseDTO;
import com.example.demo.service.PostService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Task12 โพสต์หาคนรับหิ้ว
 * GET list/detail เปิดให้ Guest ดูได้ ส่วนที่เหลือต้องมี token (userId จาก JWT)
 */
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<ApiResponseDTO<PostResponseDTO>> createPost(
            @RequestAttribute(RequestAttributes.USER_ID) Long userId,
            @Valid @RequestBody CreatePostRequestDTO request) {
        PostResponseDTO data = postService.createPost(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success("Post created successfully", data));
    }

    /** Guest ดูได้ เฉพาะโพสต์ OPEN */
    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<PostResponseDTO>>> getOpenPosts() {
        return ResponseEntity.ok(ApiResponseDTO.success(postService.getOpenPosts()));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponseDTO<List<PostResponseDTO>>> getMyPosts(@RequestAttribute(RequestAttributes.USER_ID) Long userId) {
        return ResponseEntity.ok(ApiResponseDTO.success(postService.getMyPosts(userId)));
    }

    /** Guest ดูได้ */
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponseDTO<PostResponseDTO>> getPost(@PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponseDTO.success(postService.getPost(postId)));
    }

    /** กดรับงานโพสต์ (รับโพสต์ตัวเองไม่ได้ / โพสต์ที่มีคนรับแล้วรับซ้ำไม่ได้ ตรวจที่ Service) */
    @PatchMapping("/{postId}/accept")
    public ResponseEntity<ApiResponseDTO<PostResponseDTO>> acceptPost(
            @RequestAttribute(RequestAttributes.USER_ID) Long userId,
            @PathVariable Long postId) {
        PostResponseDTO data = postService.acceptPost(userId, postId);
        return ResponseEntity.ok(ApiResponseDTO.success("Post accepted successfully", data));
    }
}
