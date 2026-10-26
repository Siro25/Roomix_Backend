package com.example.core_service.face;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

final class FaceSecrets {
    private static final SecureRandom RANDOM = new SecureRandom();
    private FaceSecrets() { }
    static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
    static boolean matches(String digest, String value) {
        return digest != null && value != null && MessageDigest.isEqual(
                digest.getBytes(StandardCharsets.US_ASCII), hash(value).getBytes(StandardCharsets.US_ASCII));
    }
}
