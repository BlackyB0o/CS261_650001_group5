package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

/**
 * Task29/31 (US02) ข้อมูลการแบ่งหน้า ส่งคู่กับ data ใน ApiResponseDTO (field "pagination")
 * page เริ่มที่ 1
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageMetaDTO {

    private int page;
    private int size;
    private long totalItems;
    private int totalPages;

    /** แปลงจาก Page ของ Spring (เริ่มที่ 0) เป็นแบบเริ่มที่ 1 */
    public static PageMetaDTO from(Page<?> page) {
        return PageMetaDTO.builder()
                .page(page.getNumber() + 1)
                .size(page.getSize())
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
