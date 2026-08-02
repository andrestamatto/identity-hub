package br.dev.andrestamatto.identityhub.identity.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

final class PasswordRecoveryDigest {

    private PasswordRecoveryDigest() { }

    static byte[] from(String secret) {
        var value = secret.getBytes(StandardCharsets.US_ASCII);
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        } finally {
            Arrays.fill(value, (byte) 0);
        }
    }
}
