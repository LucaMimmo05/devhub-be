package com.devhub.github.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Crypto for the GitHub integration:
 * - AES-256-GCM encryption of stored OAuth tokens;
 * - HMAC-signed OAuth "state", binding the authorize round-trip to the DevHub profile that started it.
 * Keys are derived from GITHUB_TOKEN_KEY (falls back to the client secret, so dev works with just the OAuth app).
 */
@ApplicationScoped
public class GithubSecrets {

    private static final int IV_BYTES = 12;
    private static final long STATE_TTL_MS = 10 * 60 * 1000;
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private final SecureRandom random = new SecureRandom();

    @Inject
    GithubConfig config;

    private byte[] key(String purpose) {
        try {
            String material = config.tokenKey().orElse(config.clientSecret().orElse(""));
            if (material.isBlank()) throw new IllegalStateException("GitHub integration is not configured");
            return MessageDigest.getInstance("SHA-256").digest((purpose + ":" + material).getBytes(StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key("token"), "AES"), new GCMParameterSpec(128, iv));
            byte[] enc = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return B64.encodeToString(ByteBuffer.allocate(iv.length + enc.length).put(iv).put(enc).array());
        } catch (Exception e) {
            throw new IllegalStateException("Could not encrypt GitHub token", e);
        }
    }

    public String decrypt(String stored) {
        try {
            byte[] all = B64D.decode(stored);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key("token"), "AES"), new GCMParameterSpec(128, all, 0, IV_BYTES));
            return new String(cipher.doFinal(all, IV_BYTES, all.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Key rotated or data corrupted: treat as a broken connection, the user reconnects.
            throw new GithubReauthException();
        }
    }

    /**
     * state = mode.base64url(subject|expiry|nonce).base64url(hmac). The mode ("connect" or "login") is readable
     * so the frontend callback page knows which flow it's finishing; it is covered by the signature.
     * subject = the DevHub profile id when connecting, "anonymous" when signing in.
     */
    public String signState(String mode, String subject) {
        byte[] nonce = new byte[12];
        random.nextBytes(nonce);
        String payload = subject + "|" + (System.currentTimeMillis() + STATE_TTL_MS) + "|" + B64.encodeToString(nonce);
        String body = mode + "." + B64.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return body + "." + B64.encodeToString(hmac(body));
    }

    public void verifyState(String state, String mode, String subject) {
        String[] parts = state == null ? new String[0] : state.split("\\.");
        if (parts.length != 3 || !parts[0].equals(mode)
                || !MessageDigest.isEqual(hmac(parts[0] + "." + parts[1]), safeDecode(parts[2]))) {
            throw new BadRequestException("Invalid GitHub authorization state. Please try again.");
        }
        String[] fields = new String(safeDecode(parts[1]), StandardCharsets.UTF_8).split("\\|");
        if (fields.length != 3 || !fields[0].equals(subject)) {
            throw new BadRequestException("This GitHub authorization was started by another account.");
        }
        if (Long.parseLong(fields[1]) < System.currentTimeMillis()) {
            throw new BadRequestException("The GitHub authorization took too long. Please try again.");
        }
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key("state"), "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] safeDecode(String s) {
        try {
            return B64D.decode(s);
        } catch (IllegalArgumentException e) {
            return new byte[0];
        }
    }
}
