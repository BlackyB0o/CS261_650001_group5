package com.example.demo.controller;

import com.example.demo.common.RequestAttributes;
import com.example.demo.dto.ApiResponseDTO;
import com.example.demo.dto.CreateDeliveryRoundRequestDTO;
import com.example.demo.dto.DeliveryRoundResponseDTO;
import com.example.demo.dto.FieldErrorDTO;
import com.example.demo.dto.RoundStatusResponseDTO;
import com.example.demo.dto.UpdateRoundStatusRequestDTO;
import com.example.demo.exception.ApiException;
import com.example.demo.service.DeliveryRoundService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
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

/**
 * Task08 รอบหิ้ว
 * GET list/detail เปิดให้ Guest ดูได้ ส่วนที่เหลือต้องมี token (userId จาก JWT)
 */
@RestController
@RequestMapping("/api/v1/delivery-rounds")
@RequiredArgsConstructor
public class DeliveryRoundController {

    private static final Set<String> STATUSES = Set.of("OPEN", "CLOSED", "COMPLETED", "CANCELLED");

    private final DeliveryRoundService deliveryRoundService;

    @PostMapping
    public ResponseEntity<ApiResponseDTO<DeliveryRoundResponseDTO>> createRound(
            @RequestAttribute(RequestAttributes.USER_ID) Long userId,
            @Valid @RequestBody CreateDeliveryRoundRequestDTO request) {
        DeliveryRoundResponseDTO data = deliveryRoundService.createRound(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success("Delivery round created successfully", data));
    }

    /** Guest ดูได้ ไม่ส่ง status = เฉพาะรอบ OPEN */
    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<DeliveryRoundResponseDTO>>> getRounds(
            @RequestParam(required = false) String status) {
        if (status != null && !STATUSES.contains(status)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Validation failed", List.of(
                    new FieldErrorDTO("status", "must be one of OPEN, CLOSED, COMPLETED, CANCELLED")));
        }
        return ResponseEntity.ok(ApiResponseDTO.success(deliveryRoundService.getRounds(status)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponseDTO<List<DeliveryRoundResponseDTO>>> getMyRounds(
            @RequestAttribute(RequestAttributes.USER_ID) Long userId) {
        return ResponseEntity.ok(ApiResponseDTO.success(deliveryRoundService.getMyRounds(userId)));
    }

    /** Guest ดูได้ */
    @GetMapping("/{roundId}")
    public ResponseEntity<ApiResponseDTO<DeliveryRoundResponseDTO>> getRound(@PathVariable Long roundId) {
        return ResponseEntity.ok(ApiResponseDTO.success(deliveryRoundService.getRound(roundId)));
    }

    /** เจ้าของรอบเท่านั้น (Service ตรวจแล้วโยน 403) */
    @PatchMapping("/{roundId}/status")
    public ResponseEntity<ApiResponseDTO<RoundStatusResponseDTO>> updateStatus(
            @RequestAttribute(RequestAttributes.USER_ID) Long userId,
            @PathVariable Long roundId,
            @Valid @RequestBody UpdateRoundStatusRequestDTO request) {
        RoundStatusResponseDTO data = deliveryRoundService.updateStatus(userId, roundId, request);
        return ResponseEntity.ok(ApiResponseDTO.success("Delivery round status updated successfully", data));
    }
}
