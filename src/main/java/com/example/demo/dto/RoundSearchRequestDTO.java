package com.example.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Task29/31 (US02) เงื่อนไขค้นหารอบหิ้ว รับจาก query string ของ GET /api/v1/delivery-rounds
 * ทุกช่องไม่บังคับ ไม่ส่งมาเลย = รอบ OPEN หน้าแรก หน้าละ 20 รอบ
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoundSearchRequestDTO {

    /** ไม่ส่ง = OPEN */
    @Pattern(regexp = "OPEN|CLOSED|COMPLETED|CANCELLED",
            message = "must be one of OPEN, CLOSED, COMPLETED, CANCELLED")
    private String status;

    /** ค้นจากชื่อร้าน สถานที่ หรือชื่อผู้รับหิ้ว (ไม่สนตัวพิมพ์เล็กใหญ่) */
    @Size(max = 100, message = "must be at most 100 characters")
    private String keyword;

    /** วันที่ของ scheduledAt รูปแบบ yyyy-MM-dd */
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "must be a valid date in yyyy-MM-dd format")
    private String date;

    /** หน้าที่ต้องการ เริ่มที่ 1 ไม่ส่ง = 1 */
    @Min(value = 1, message = "must be greater than or equal to 1")
    private Integer page;

    /** จำนวนรอบต่อหน้า 1 ถึง 100 ไม่ส่ง = 20 */
    @Min(value = 1, message = "must be between 1 and 100")
    @Max(value = 100, message = "must be between 1 and 100")
    private Integer size;
}
