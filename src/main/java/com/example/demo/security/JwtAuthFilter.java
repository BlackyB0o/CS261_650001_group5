package com.example.demo.security;

import com.example.demo.common.RequestAttributes;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * อ่าน "Authorization: Bearer <token>" ถ้า token ถูกต้องและบัญชียังมีอยู่ จะตั้ง request attribute "userId"
 * ไม่ปฏิเสธ request เอง: endpoint ของ Guest ใช้ได้ตามปกติ
 * ส่วน endpoint ที่ต้องล็อกอินจะตอบ 401 เองเมื่อไม่มี userId (ดู GlobalExceptionHandler)
 * Task56 (US10) ยกเว้นบัญชีที่ถูกแอดมินระงับ ตอบ 401 "Account suspended" ทันที (token เดิมใช้ต่อไม่ได้)
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String SUSPENDED_BODY = "{\"status\":\"ERROR\",\"message\":\"Account suspended\"}";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            Long userId = jwtService.getUserId(header.substring(7).trim());
            User user = userId == null ? null : userRepository.findById(userId).orElse(null);
            if (user != null && user.isSuspended()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write(SUSPENDED_BODY);
                return;
            }
            if (user != null) {
                request.setAttribute(RequestAttributes.USER_ID, userId);
            }
        }
        filterChain.doFilter(request, response);
    }
}
