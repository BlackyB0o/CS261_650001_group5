package com.example.demo.service.impl;

import com.example.demo.dto.CreateDeliveryRoundRequestDTO;
import com.example.demo.dto.DeliveryRoundResponseDTO;
import com.example.demo.dto.RoundStatusResponseDTO;
import com.example.demo.dto.UpdateRoundStatusRequestDTO;
import com.example.demo.entity.DeliveryRound;
import com.example.demo.entity.User;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.DeliveryRoundRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.DeliveryRoundService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Task08 รอบหิ้ว เจ้าของรอบ = ผู้รับหิ้วของรอบนั้น */
@Service
@RequiredArgsConstructor
public class DeliveryRoundServiceImpl implements DeliveryRoundService {

    /** การเปลี่ยนสถานะที่อนุญาต (ตารางในเอกสาร Task08) */
    private static final Map<String, Set<String>> ALLOWED = Map.of(
            "OPEN", Set.of("CLOSED", "CANCELLED"),
            "CLOSED", Set.of("COMPLETED", "CANCELLED"),
            "COMPLETED", Set.of(),
            "CANCELLED", Set.of());

    private final DeliveryRoundRepository deliveryRoundRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public DeliveryRoundResponseDTO createRound(Long runnerId, CreateDeliveryRoundRequestDTO request) {
        User runner = userRepository.findById(runnerId)
                .orElseThrow(() -> ApiException.unauthorized("Invalid or missing token"));
        DeliveryRound round = new DeliveryRound();
        round.setRunner(runner);
        round.setStoreName(request.getStoreName().trim());
        round.setLocationName(request.getLocationName().trim());
        round.setScheduledAt(request.getScheduledAt());
        round.setServiceFee(request.getServiceFee());
        round.setStatus("OPEN");
        return toDto(deliveryRoundRepository.save(round));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryRoundResponseDTO> getRounds(String status) {
        String filter = status == null ? "OPEN" : status;
        return deliveryRoundRepository.findByStatusOrderByScheduledAtAsc(filter).stream()
                .map(DeliveryRoundServiceImpl::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryRoundResponseDTO> getMyRounds(Long runnerId) {
        return deliveryRoundRepository.findByRunnerIdOrderByScheduledAtDesc(runnerId).stream()
                .map(DeliveryRoundServiceImpl::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryRoundResponseDTO getRound(Long roundId) {
        return toDto(findRound(roundId));
    }

    @Override
    @Transactional
    public RoundStatusResponseDTO updateStatus(Long runnerId, Long roundId, UpdateRoundStatusRequestDTO request) {
        DeliveryRound round = findRound(roundId);
        if (!round.getRunner().getId().equals(runnerId)) {
            throw ApiException.forbidden("You are not the owner of this delivery round");
        }
        String from = round.getStatus();
        String to = request.getStatus();
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw ApiException.conflict("Cannot change status from " + from + " to " + to);
        }
        round.setStatus(to);
        deliveryRoundRepository.save(round);
        return RoundStatusResponseDTO.builder().roundId(round.getId()).status(to).build();
    }

    private DeliveryRound findRound(Long roundId) {
        return deliveryRoundRepository.findById(roundId)
                .orElseThrow(() -> ApiException.notFound("Delivery round not found"));
    }

    static DeliveryRoundResponseDTO toDto(DeliveryRound round) {
        return DeliveryRoundResponseDTO.builder()
                .roundId(round.getId())
                .runnerId(round.getRunner().getId())
                .runnerName(round.getRunner().getFullName())
                .storeName(round.getStoreName())
                .locationName(round.getLocationName())
                .scheduledAt(round.getScheduledAt())
                .serviceFee(round.getServiceFee())
                .status(round.getStatus())
                .build();
    }
}
