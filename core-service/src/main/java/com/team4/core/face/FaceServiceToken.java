package com.team4.core.face;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FaceServiceToken {
    private final String digest;
    public FaceServiceToken(@Value("${core.face.service-token}") String token) {
        if (token.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("FACE_SERVICE_TOKEN needs at least 32 UTF-8 bytes");
        }
        digest = FaceSecrets.hash(token);
    }
    public boolean accepts(String token) { return FaceSecrets.matches(digest, token); }
}
