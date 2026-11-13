package com.example.demo.common;

import java.util.Map;
import java.util.Set;

/**
 * Task46 (US06) กติกาสถานะออเดอร์
 * PENDING -> PAID -> ACCEPTED -> PURCHASING -> READY_FOR_DELIVERY -> DELIVERING -> COMPLETED
 * PENDING -> CANCELLED
 * PAID เกิดตอนผู้ฝากหิ้วจ่ายเงิน (US07) และ COMPLETED เกิดตอนผู้ฝากหิ้วยืนยันรับสินค้า (US08)
 */
public final class OrderStatusRules {

    /** ทุกสถานะที่มีในระบบ ใช้ตรวจค่าที่ส่งมาใน request */
    public static final String ALL_STATUSES =
            "PENDING|PAID|ACCEPTED|PURCHASING|READY_FOR_DELIVERY|DELIVERING|COMPLETED|CANCELLED";

    /** สถานะที่ผู้รับหิ้วเปลี่ยนได้ ทีละขั้น ห้ามข้ามหรือย้อนกลับ (US06) */
    public static final Map<String, Set<String>> RUNNER_TRANSITIONS = Map.of(
            "PENDING", Set.of("CANCELLED"),
            "PAID", Set.of("ACCEPTED"),
            "ACCEPTED", Set.of("PURCHASING"),
            "PURCHASING", Set.of("READY_FOR_DELIVERY"),
            "READY_FOR_DELIVERY", Set.of("DELIVERING"));

    /** สถานะที่ผู้ฝากหิ้วยืนยันรับสินค้าได้ คือจ่ายเงินแล้วแต่ยังไม่เสร็จ (US08) */
    public static final Set<String> CONFIRMABLE =
            Set.of("PAID", "ACCEPTED", "PURCHASING", "READY_FOR_DELIVERY", "DELIVERING");

    private OrderStatusRules() {
    }

    public static boolean canRunnerChange(String from, String to) {
        return RUNNER_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
