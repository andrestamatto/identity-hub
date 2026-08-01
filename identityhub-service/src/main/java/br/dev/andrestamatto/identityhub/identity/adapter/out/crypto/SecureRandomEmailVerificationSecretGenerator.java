package br.dev.andrestamatto.identityhub.identity.adapter.out.crypto;

import br.dev.andrestamatto.identityhub.identity.application.EmailVerificationSecret;
import br.dev.andrestamatto.identityhub.identity.application.EmailVerificationSecretGenerator;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

public final class SecureRandomEmailVerificationSecretGenerator
        implements EmailVerificationSecretGenerator {

    private static final int SECRET_BYTES = 32;

    private final SecureRandom secureRandom;

    public SecureRandomEmailVerificationSecretGenerator(SecureRandom secureRandom) {
        this.secureRandom = Objects.requireNonNull(secureRandom);
    }

    @Override
    public EmailVerificationSecret generate() {
        var bytes = new byte[SECRET_BYTES];
        try {
            secureRandom.nextBytes(bytes);
            return new EmailVerificationSecret(
                    Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
        } finally {
            Arrays.fill(bytes, (byte) 0);
        }
    }
}
