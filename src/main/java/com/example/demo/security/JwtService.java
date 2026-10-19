package com.example.demo.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * สร้างและตรวจ JWT (HS256) ที่มีเฉพาะ userId (Task04)
 * เขียนด้วย javax.crypto ของ Java เอง ไม่ต้องเพิ่ม dependency
 */
@Component
public class JwtService {

    private static final Pattern SUB = Pattern.compile("\"sub\"\\s*:\\s*\"?(\\d+)\"?");
    private static final Pattern EXP = Pattern.compile("\"exp\"\\s*:\\s*(\\d+)");

    private final byte[] secret;
    private final long expirationSeconds;

    public JwtService(
            @Value("${app.jwt.secret:carryhub-dev-secret-change-me-at-least-32-bytes}") String secret,
            @Value("${app.jwt.expiration-minutes:1440}") long expirationMinutes) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationMinutes * 60;
    }

    public String generateToken(Long userId) {
        long now = Instant.now().getEpochSecond();
        String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = encode("{\"sub\":\"" + userId + "\",\"iat\":" + now
                + ",\"exp\":" + (now + expirationSeconds) + "}");
        return header + "." + payload + "." + sign(header + "." + payload);
    }

    /** คืน userId ถ้า token ถูกต้องและยังไม่หมดอายุ ไม่งั้นคืน null */
    public Long getUserId(String token) {
        if (token == null) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }
        String expected = sign(parts[0] + "." + parts[1]);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            return null;
        }
        String json;
        try {
            json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
        Long userId = find(SUB, json);
        Long exp = find(EXP, json);
        if (userId == null || exp == null || exp < Instant.now().getEpochSecond()) {
            return null;
        }
        return userId;
    }

    private static Long find(Pattern pattern, String json) {
        Matcher m = pattern.matcher(json);
        if (!m.find()) {
            return null;
        }
        try {
            return Long.parseLong(m.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot sign JWT", e);
        }
    }

    private static String encode(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
