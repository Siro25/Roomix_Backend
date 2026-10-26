package com.example.core_service.auth;

import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.example.core_service.auth.AuthDtos.AuthResponse;
import com.example.core_service.auth.AuthDtos.LoginRequest;
import com.example.core_service.auth.AuthDtos.UserProfile;

@RestController
public class AdminAuthController {
    private final AdminAuthService auth;

    public AdminAuthController(AdminAuthService auth) { this.auth = auth; }

    @PostMapping("/api/auth/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(auth.login(request.username(), request.password()));
    }

    @GetMapping("/api/users/me")
    public ResponseEntity<UserProfile> profile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(auth.profile(jwt.getSubject()));
    }
}
