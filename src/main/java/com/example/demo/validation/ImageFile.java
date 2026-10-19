package com.example.demo.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ตรวจไฟล์รูปที่อัปโหลด (สลิป Task20, หลักฐานรับสินค้า Task24)
 * ต้องแนบและไม่ว่าง, เป็น JPEG หรือ PNG, ไม่เกิน 5 MB
 */
@Documented
@Constraint(validatedBy = ImageFileValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ImageFile {

    String message() default "invalid image file";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
