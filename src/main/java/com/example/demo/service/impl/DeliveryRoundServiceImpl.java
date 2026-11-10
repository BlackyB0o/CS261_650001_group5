package com.example.demo.service.impl;

import com.example.demo.dto.CreateDeliveryRoundRequestDTO;
import com.example.demo.dto.DeliveryRoundResponseDTO;
import com.example.demo.dto.RoundSearchRequestDTO;
import com.example.demo.dto.RoundStatusResponseDTO;
import com.example.demo.dto.UpdateRoundStatusRequestDTO;
import com.example.demo.entity.DeliveryRound;
import com.example.demo.entity.User;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.DeliveryRoundRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.DeliveryRoundService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    /** จำนวนรอบต่อหน้าเมื่อไม่ส่ง size (Task31) */
    static final int DEFAULT_PAGE_SIZE = 20;

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
    public Page<DeliveryRoundResponseDTO> getRounds(RoundSearchRequestDTO criteria) {
        String status = criteria.getStatus() == null ? "OPEN" : criteria.getStatus();
        String keyword = toLikePattern(criteria.getKeyword());
        int page = criteria.getPage() == null ? 1 : criteria.getPage();
        int size = criteria.getSize() == null ? DEFAULT_PAGE_SIZE : criteria.getSize();
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<DeliveryRound> rounds;
        if (criteria.getDate() == null || criteria.getDate().isBlank()) {
            rounds = deliveryRoundRepository.search(status, keyword, pageable);
        } else {
            LocalDate day = parseDate(criteria.getDate());
            rounds = deliveryRoundRepository.searchByDate(status, keyword,
                    day.atStartOfDay(), day.plusDays(1).atStartOfDay(), pageable);
        }
        return rounds.map(DeliveryRoundServiceImpl::toDto);
    }

    /** คำค้นว่างหรือไม่ส่ง = ทุกรอบ ("%") ส่วน % _ ! ที่ผู้ใช้พิมพ์มาให้ถือเป็นตัวอักษรธรรมดา */
    static String toLikePattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "%";
        }
        String escaped = keyword.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + escaped + "%";
    }

    /** DTO ตรวจรูปแบบ yyyy-MM-dd แล้ว ที่นี่กันวันที่ไม่มีจริง เช่น 2026-02-30 */
    private static LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw ApiException.validation("date", "must be a valid date in yyyy-MM-dd format");
        }
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
