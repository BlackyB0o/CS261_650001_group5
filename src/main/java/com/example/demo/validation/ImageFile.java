package com.example.demo.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ตรวจไฟล์รูปที่อัปโหลด (สลิป Task20, หลักฐานรับสินค้า Task24, หลักฐานรายงานปัญหา Task51)
 * เป็น JPEG หรือ PNG, ไม่เกิน 5 MB และต้องแนบ (ยกเว้น required = false ซึ่งไม่แนบก็ได้)
 */
@Documented
@Constraint(validatedBy = ImageFileValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ImageFile {

    String message() default "invalid image file";

    /** false = ไม่แนบไฟล์ก็ได้ แต่ถ้าแนบต้องเป็นรูปที่ถูกต้อง (Task51 US09) */
    boolean required() default true;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
