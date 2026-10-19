package com.example.demo.common;

/**
 * ชื่อ request attribute ที่ JWT filter (Task04) ต้องตั้งค่าหลังตรวจ token สำเร็จ
 * Controller อ่าน userId จากตรงนี้เท่านั้น ห้ามรับจาก request body
 * ถ้าไม่มีค่านี้ (ไม่มี token หรือ token ไม่ถูกต้อง) ระบบตอบ 401 "Invalid or missing token"
 */
public final class RequestAttributes {

    public static final String USER_ID = "userId";

    private RequestAttributes() {
    }
}
