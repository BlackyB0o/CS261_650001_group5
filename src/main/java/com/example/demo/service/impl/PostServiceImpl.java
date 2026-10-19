package com.example.demo.service.impl;

import com.example.demo.dto.CreatePostRequestDTO;
import com.example.demo.dto.PostResponseDTO;
import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.PostService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Task12 โพสต์หาคนรับหิ้ว เจ้าของโพสต์ = ผู้ฝากหิ้ว, คนกดรับ = ผู้รับหิ้วของโพสต์นั้น */
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public PostResponseDTO createPost(Long buyerId, CreatePostRequestDTO request) {
        User buyer = findUser(buyerId);
        Post post = new Post();
        post.setBuyer(buyer);
        post.setPostTitle(request.getPostTitle().trim());
        post.setShopName(request.getShopName().trim());
        post.setProductDetail(request.getProductDetail().trim());
        post.setQuantity(request.getQuantity());
        post.setCarryFee(request.getCarryFee());
        post.setDesiredDate(request.getDesiredDate());
        post.setLocation(request.getLocation().trim());
        post.setBudget(request.getBudget());
        post.setAdditionalDetails(request.getAdditionalDetails());
        post.setStatus("OPEN");
        return toDto(postRepository.save(post));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponseDTO> getOpenPosts() {
        return postRepository.findByStatusOrderByCreatedAtDesc("OPEN").stream()
                .map(PostServiceImpl::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponseDTO> getMyPosts(Long buyerId) {
        return postRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId).stream()
                .map(PostServiceImpl::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponseDTO getPost(Long postId) {
        return toDto(findPost(postId));
    }

    @Override
    @Transactional
    public PostResponseDTO acceptPost(Long userId, Long postId) {
        Post post = findPost(postId);
        if (post.getBuyer().getId().equals(userId)) {
            throw ApiException.forbidden("You cannot accept your own post");
        }
        if (!"OPEN".equals(post.getStatus())) {
            throw ApiException.conflict("Post is no longer open");
        }
        post.setAcceptedBy(findUser(userId));
        post.setAcceptedAt(LocalDateTime.now());
        post.setStatus("ACCEPTED");
        return toDto(postRepository.save(post));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("Invalid or missing token"));
    }

    private Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> ApiException.notFound("Post not found"));
    }

    private static PostResponseDTO toDto(Post post) {
        User accepted = post.getAcceptedBy();
        return PostResponseDTO.builder()
                .postId(post.getId())
                .postTitle(post.getPostTitle())
                .shopName(post.getShopName())
                .productDetail(post.getProductDetail())
                .quantity(post.getQuantity())
                .carryFee(post.getCarryFee())
                .desiredDate(post.getDesiredDate())
                .location(post.getLocation())
                .budget(post.getBudget())
                .additionalDetails(post.getAdditionalDetails())
                .status(post.getStatus())
                .buyerId(post.getBuyer().getId())
                .buyerName(post.getBuyer().getFullName())
                .acceptedBy(accepted == null ? null : accepted.getId())
                .acceptedByName(accepted == null ? null : accepted.getFullName())
                .acceptedAt(post.getAcceptedAt())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
