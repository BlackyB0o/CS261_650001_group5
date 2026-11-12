package com.example.demo.service;

import com.example.demo.dto.CreateOrderRequestDTO;
import com.example.demo.dto.OrderResponseDTO;
import java.util.List;

/** Task16 */
public interface OrderService {

    /** 403 ฝากซื้อในรอบของตัวเอง, 409 รอบไม่ใช่ OPEN */
    OrderResponseDTO createOrder(Long buyerId, CreateOrderRequestDTO request);

    List<OrderResponseDTO> getMyOrders(Long buyerId);

    /**
     * เฉพาะเจ้าของรอบ (403 ถ้าไม่ใช่, 404 ถ้าไม่พบรอบ)
     * ใช้กับ GET /orders?roundId= (Task16) และ GET /delivery-rounds/{roundId}/orders (Task36 US04)
     */
    List<OrderResponseDTO> getOrdersByRound(Long runnerId, Long roundId);

    /** เจ้าของออเดอร์ หรือเจ้าของรอบที่ออเดอร์อยู่ */
    OrderResponseDTO getOrder(Long userId, Long orderId);
}
