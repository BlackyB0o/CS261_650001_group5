package com.example.demo.service.impl;

import com.example.demo.dto.CreateOrderRequestDTO;
import com.example.demo.dto.OrderItemRequestDTO;
import com.example.demo.dto.OrderItemResponseDTO;
import com.example.demo.dto.OrderResponseDTO;
import com.example.demo.entity.DeliveryRound;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.User;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.DeliveryRoundRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.OrderService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Task16 ออเดอร์ เจ้าของออเดอร์ = ผู้ฝากหิ้ว, เจ้าของรอบของออเดอร์ = ผู้รับหิ้ว */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final DeliveryRoundRepository deliveryRoundRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public OrderResponseDTO createOrder(Long buyerId, CreateOrderRequestDTO request) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> ApiException.unauthorized("Invalid or missing token"));
        DeliveryRound round = findRound(request.getRoundId());
        if (round.getRunner().getId().equals(buyerId)) {
            throw ApiException.forbidden("You cannot order from your own round");
        }
        if (!"OPEN".equals(round.getStatus())) {
            throw ApiException.conflict("Delivery round is not open");
        }
        Order order = new Order();
        order.setBuyer(buyer);
        order.setRound(round);
        order.setHandoffMethod(request.getHandoffMethod());
        order.setHandoffLocation(request.getHandoffLocation().trim());
        order.setStatus("PENDING");
        for (OrderItemRequestDTO itemRequest : request.getItems()) {
            OrderItem item = new OrderItem();
            item.setProductName(itemRequest.getProductName().trim());
            item.setQuantity(itemRequest.getQuantity());
            item.setNote(itemRequest.getNote());
            order.addItem(item);
        }
        return toDto(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getMyOrders(Long buyerId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId).stream()
                .map(OrderServiceImpl::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByRound(Long runnerId, Long roundId) {
        DeliveryRound round = findRound(roundId);
        if (!round.getRunner().getId().equals(runnerId)) {
            throw ApiException.forbidden("You are not the owner of this delivery round");
        }
        return orderRepository.findByRoundIdOrderByCreatedAtAsc(roundId).stream()
                .map(OrderServiceImpl::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        boolean isBuyer = order.getBuyer().getId().equals(userId);
        boolean isRunner = order.getRound().getRunner().getId().equals(userId);
        if (!isBuyer && !isRunner) {
            throw ApiException.forbidden("You are not allowed to view this order");
        }
        return toDto(order);
    }

    private DeliveryRound findRound(Long roundId) {
        return deliveryRoundRepository.findById(roundId)
                .orElseThrow(() -> ApiException.notFound("Delivery round not found"));
    }

    static OrderResponseDTO toDto(Order order) {
        List<OrderItemResponseDTO> items = order.getItems().stream()
                .map(item -> OrderItemResponseDTO.builder()
                        .itemId(item.getId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .note(item.getNote())
                        .build())
                .toList();
        return OrderResponseDTO.builder()
                .orderId(order.getId())
                .roundId(order.getRound().getId())
                .buyerId(order.getBuyer().getId())
                .buyerName(order.getBuyer().getFullName())
                .handoffMethod(order.getHandoffMethod())
                .handoffLocation(order.getHandoffLocation())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .items(items)
                .build();
    }
}
