package com.example.demo.service;

import com.example.demo.dto.CreateOrderRequestDTO;
import com.example.demo.dto.OrderDetailResponseDTO;
import com.example.demo.dto.OrderResponseDTO;
import com.example.demo.dto.OrderStatusResponseDTO;
import com.example.demo.dto.UpdateOrderStatusRequestDTO;
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

    /**
     * Task41 (US05) รายละเอียดออเดอร์ พร้อมผู้รับหิ้วและประวัติสถานะ
     * ดูได้เฉพาะผู้ฝากหิ้วของออเดอร์ หรือผู้รับหิ้วของรอบ (403), ไม่พบ (404)
     */
    OrderDetailResponseDTO getOrderDetail(Long userId, Long orderId);

    /**
     * Task46 (US06) ผู้รับหิ้วเจ้าของรอบเปลี่ยนสถานะออเดอร์ทีละขั้นตาม OrderStatusRules
     * 404 ไม่พบออเดอร์, 403 ไม่ใช่เจ้าของรอบ, 409 เปลี่ยนจากสถานะเดิมไปสถานะนี้ไม่ได้
     */
    OrderStatusResponseDTO updateStatus(Long runnerId, Long orderId, UpdateOrderStatusRequestDTO request);
}
