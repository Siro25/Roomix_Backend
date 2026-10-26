package com.team4.core.security;

import com.team4.core.enums.UserStatus;
import com.team4.core.exception.GlobalExceptionHandler;
import com.team4.core.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class AccountStatusFilter extends OncePerRequestFilter {
    private final UserRepository userRepository;
    private final GlobalExceptionHandler exceptionHandler;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean active = userRepository.findById(UUID.fromString(jwtAuthentication.getToken().getSubject()))
                .map(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElse(false);
        if (active) {
            filterChain.doFilter(request, response);
            return;
        }

        SecurityContextHolder.clearContext();
        exceptionHandler.commence(
                request,
                response,
                new BadCredentialsException("Account is not active"));
    }
}
