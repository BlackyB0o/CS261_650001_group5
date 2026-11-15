package com.example.demo.common;

/** Task54 (US10) สถานะของรายงาน รายงานใหม่เป็น PENDING แล้วแอดมินพิจารณาเป็น APPROVED หรือ DECLINED */
public final class ReportStatuses {

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String DECLINED = "DECLINED";

    /** ผลการพิจารณาที่แอดมินส่งได้ */
    public static final String DECISIONS = "APPROVED|DECLINED";

    private ReportStatuses() {
    }
}
