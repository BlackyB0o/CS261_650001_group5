package com.example.demo.controller;

import com.example.demo.common.RequestAttributes;
import com.example.demo.dto.ApiResponseDTO;
import com.example.demo.dto.CreateOrderRequestDTO;
import com.example.demo.dto.OrderDetailResponseDTO;
import com.example.demo.dto.OrderResponseDTO;
import com.example.demo.dto.OrderStatusResponseDTO;
import com.example.demo.dto.UpdateOrderStatusRequestDTO;
import com.example.demo.service.OrderService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Task16 ออเดอร์ + Task41 (US05) รายละเอียด + Task46 (US06) เปลี่ยนสถานะ ทุก endpoint ต้องมี token (userId จาก JWT) */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponseDTO<OrderResponseDTO>> createOrder(
            @RequestAttribute(RequestAttributes.USER_ID) Long userId,
            @Valid @RequestBody CreateOrderRequestDTO request) {
        OrderResponseDTO data = orderService.createOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success("Order created successfully", data));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponseDTO<List<OrderResponseDTO>>> getMyOrders(@RequestAttribute(RequestAttributes.USER_ID) Long userId) {
        return ResponseEntity.ok(ApiResponseDTO.success(orderService.getMyOrders(userId)));
    }

    /** ออเดอร์ทั้งหมดในรอบ (เจ้าของรอบเท่านั้น) ไม่ส่ง roundId = 400 "roundId is required" */
    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<OrderResponseDTO>>> getOrdersByRound(
            @RequestAttribute(RequestAttributes.USER_ID) Long userId,
            @RequestParam Long roundId) {
        return ResponseEntity.ok(ApiResponseDTO.success(orderService.getOrdersByRound(userId, roundId)));
    }

    /** Task41 (US05) รายละเอียดออเดอร์ พร้อมผู้รับหิ้วและประวัติสถานะ (ผู้ฝากหิ้วหรือผู้รับหิ้วของรอบ) */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponseDTO<OrderDetailResponseDTO>> getOrderDetail(
            @RequestAttribute(RequestAttributes.USER_ID) Long userId,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponseDTO.success(orderService.getOrderDetail(userId, orderId)));
    }

    /** Task46 (US06) ผู้รับหิ้วเจ้าของรอบเปลี่ยนสถานะออเดอร์ทีละขั้น */
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<ApiResponseDTO<OrderStatusResponseDTO>> updateStatus(
            @RequestAttribute(RequestAttributes.USER_ID) Long userId,
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequestDTO request) {
        return ResponseEntity.ok(ApiResponseDTO.success("Order status updated successfully",
                orderService.updateStatus(userId, orderId, request)));
    }
}
