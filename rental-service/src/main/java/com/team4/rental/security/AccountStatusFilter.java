package com.team4.rental.security;

import com.team4.rental.dtos.response.ApiResponse;
import com.team4.rental.exceptions.*;
import com.team4.rental.external.CoreServiceClient;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class AccountStatusFilter extends OncePerRequestFilter {
    private final CoreServiceClient coreServiceClient;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/rentals/admin/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt) {
            try {
                var account = coreServiceClient.getCurrentAccount(jwt.getToken().getTokenValue());
                if (!"ACTIVE".equals(account.getStatus())
                        || !UUID.fromString(jwt.getToken().getSubject()).equals(account.getId())
                        || !jwt.getToken().getClaimAsString("role").equals(account.getRole())) {
                    throw new AppException(ErrorCode.UNAUTHENTICATED);
                }
            } catch (AppException exception) {
                SecurityContextHolder.clearContext();
                var error = exception.getErrorCode();
                response.setStatus(error.getStatus().value());
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                objectMapper.writeValue(response.getOutputStream(), ApiResponse.<Void>builder()
                        .code(error.getCode()).message(error.getMessage()).build());
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
