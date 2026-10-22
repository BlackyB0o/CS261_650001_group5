package com.example.demo.security;

import com.example.demo.common.RequestAttributes;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * อ่าน "Authorization: Bearer <token>" ถ้า token ถูกต้องจะตั้ง request attribute "userId"
 * ไม่ปฏิเสธ request เอง: endpoint ของ Guest ใช้ได้ตามปกติ
 * ส่วน endpoint ที่ต้องล็อกอินจะตอบ 401 เองเมื่อไม่มี userId (ดู GlobalExceptionHandler)
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            Long userId = jwtService.getUserId(header.substring(7).trim());
            if (userId != null) {
                request.setAttribute(RequestAttributes.USER_ID, userId);
            }
        }
        filterChain.doFilter(request, response);
    }
}
