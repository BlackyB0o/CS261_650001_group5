package com.example.demo.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * เก็บไฟล์รูปที่อัปโหลด (สลิป Task20, หลักฐานรับสินค้า Task24) ลงโฟลเดอร์ app.upload-dir
 * คืนค่าที่อยู่แบบ /uploads/{folder}/{ชื่อไฟล์} ซึ่งเปิดดูผ่าน http://host:port/uploads/... ได้
 */
@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file, String folder) {
        String extension = "image/png".equalsIgnoreCase(file.getContentType()) ? ".png" : ".jpg";
        String fileName = UUID.randomUUID() + extension;
        try {
            Path dir = root.resolve(folder);
            Files.createDirectories(dir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not store file", e);
        }
        return "/uploads/" + folder + "/" + fileName;
    }

    public Path getRoot() {
        return root;
    }
}
