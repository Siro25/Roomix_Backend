package com.example.core_service.face;

import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.example.core_service.auth.AuthDtos.AuthResponse;
import com.example.core_service.face.FaceDtos.*;

@RestController
public class FaceAuthController {
    private final FaceSessionService sessions;
    public FaceAuthController(FaceSessionService sessions) { this.sessions = sessions; }
    private <T> ResponseEntity<T> response(T body) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body); }
    @PostMapping("/api/auth/face/sessions")
    public ResponseEntity<SessionResponse> create(@Valid @RequestBody CreateRequest request, @AuthenticationPrincipal Jwt jwt) {
        return response(sessions.create(request.purpose(), jwt));
    }
    @PostMapping("/internal/face/sessions/validate")
    public ResponseEntity<ValidateResponse> validate(@Valid @RequestBody ValidateRequest request, @AuthenticationPrincipal Jwt jwt) {
        return response(sessions.validate(request, jwt));
    }
    @PostMapping("/internal/face/results")
    public ResponseEntity<Decision> submit(@Valid @RequestBody ResultRequest request) { return response(sessions.submit(request)); }
    @PostMapping("/api/auth/face/complete")
    public ResponseEntity<AuthResponse> complete(@Valid @RequestBody CompleteRequest request) { return response(sessions.complete(request)); }
}
