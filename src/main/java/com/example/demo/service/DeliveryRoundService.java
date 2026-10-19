package com.example.demo.service;

import com.example.demo.dto.CreateDeliveryRoundRequestDTO;
import com.example.demo.dto.DeliveryRoundResponseDTO;
import com.example.demo.dto.RoundStatusResponseDTO;
import com.example.demo.dto.UpdateRoundStatusRequestDTO;
import java.util.List;

/** Task08 */
public interface DeliveryRoundService {

    DeliveryRoundResponseDTO createRound(Long runnerId, CreateDeliveryRoundRequestDTO request);

    /** status เป็น null = ได้เฉพาะรอบ OPEN */
    List<DeliveryRoundResponseDTO> getRounds(String status);

    List<DeliveryRoundResponseDTO> getMyRounds(Long runnerId);

    DeliveryRoundResponseDTO getRound(Long roundId);

    RoundStatusResponseDTO updateStatus(Long runnerId, Long roundId, UpdateRoundStatusRequestDTO request);
}
