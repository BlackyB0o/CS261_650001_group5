package com.example.demo.service;

import com.example.demo.dto.CreateDeliveryRoundRequestDTO;
import com.example.demo.dto.DeliveryRoundResponseDTO;
import com.example.demo.dto.RoundSearchRequestDTO;
import com.example.demo.dto.RoundStatusResponseDTO;
import com.example.demo.dto.UpdateRoundStatusRequestDTO;
import java.util.List;
import org.springframework.data.domain.Page;

/** Task08 */
public interface DeliveryRoundService {

    DeliveryRoundResponseDTO createRound(Long runnerId, CreateDeliveryRoundRequestDTO request);

    /**
     * Task08 + Task31 (US02) ดึงรายการรอบ status เป็น null = ได้เฉพาะรอบ OPEN
     * keyword, date, page, size ไม่บังคับ ได้ผลเป็นหน้า (page เริ่มที่ 1 หน้าละ 20 รอบถ้าไม่ระบุ)
     */
    Page<DeliveryRoundResponseDTO> getRounds(RoundSearchRequestDTO criteria);

    List<DeliveryRoundResponseDTO> getMyRounds(Long runnerId);

    DeliveryRoundResponseDTO getRound(Long roundId);

    RoundStatusResponseDTO updateStatus(Long runnerId, Long roundId, UpdateRoundStatusRequestDTO request);
}
