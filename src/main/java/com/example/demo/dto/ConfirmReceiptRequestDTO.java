package com.example.demo.dto;

import com.example.demo.validation.ImageFile;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/** POST /api/v1/orders/{orderId}/completion (multipart/form-data ผูกด้วย @ModelAttribute) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmReceiptRequestDTO {

    @ImageFile
    private MultipartFile evidenceImage;
}
