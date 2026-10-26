package com.example.core_service.face;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

// Thêm đúng một lần vào SecurityFilterChain, không đăng ký servlet filter riêng.
public class FaceServiceTokenFilter extends OncePerRequestFilter {
    private final FaceServiceToken tokens;
    public FaceServiceTokenFilter(FaceServiceToken tokens) { this.tokens = tokens; }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if (path.isEmpty()) path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/internal/face/")
                && !tokens.accepts(request.getHeader("X-Service-Token"))) {
            response.setStatus(401);
            response.setContentType("application/json");
            response.setHeader("Cache-Control", "no-store");
            response.getWriter().write("{\"code\":\"UNAUTHORIZED\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
