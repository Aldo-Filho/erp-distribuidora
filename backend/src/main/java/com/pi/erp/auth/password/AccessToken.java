package com.pi.erp.auth.password;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

public final class AccessToken {

    private static final SecureRandom RANDOM = new SecureRandom();

    private AccessToken() {}

    public static String create() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String value) {
        if (value == null || value.length() > 512) {
            throw new IllegalArgumentException("Invalid or expired link.");
        }
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Token hashing is unavailable.");
        }
    }

    public static String email(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public static void password(String value) {
        if (
            value == null ||
            value.codePointCount(0, value.length()) < 15 ||
            value.codePointCount(0, value.length()) > 128
        ) {
            throw new IllegalArgumentException(
                "Password must contain between 15 and 128 characters."
            );
        }
    }
}
