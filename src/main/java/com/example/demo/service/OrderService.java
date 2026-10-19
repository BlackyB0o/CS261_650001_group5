package com.example.demo.service;

import com.example.demo.dto.CreateOrderRequestDTO;
import com.example.demo.dto.OrderResponseDTO;
import java.util.List;

/** Task16 */
public interface OrderService {

    /** 403 ฝากซื้อในรอบของตัวเอง, 409 รอบไม่ใช่ OPEN */
    OrderResponseDTO createOrder(Long buyerId, CreateOrderRequestDTO request);

    List<OrderResponseDTO> getMyOrders(Long buyerId);

    /** เฉพาะเจ้าของรอบ */
    List<OrderResponseDTO> getOrdersByRound(Long runnerId, Long roundId);

    /** เจ้าของออเดอร์ หรือเจ้าของรอบที่ออเดอร์อยู่ */
    OrderResponseDTO getOrder(Long userId, Long orderId);
}
