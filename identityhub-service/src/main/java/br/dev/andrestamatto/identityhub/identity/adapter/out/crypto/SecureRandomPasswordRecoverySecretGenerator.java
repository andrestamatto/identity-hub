package br.dev.andrestamatto.identityhub.identity.adapter.out.crypto;

import br.dev.andrestamatto.identityhub.identity.application.PasswordRecoverySecret;
import br.dev.andrestamatto.identityhub.identity.application.PasswordRecoverySecretGenerator;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

public final class SecureRandomPasswordRecoverySecretGenerator
        implements PasswordRecoverySecretGenerator {

    private static final int SECRET_BYTES = 32;

    private final SecureRandom secureRandom;

    public SecureRandomPasswordRecoverySecretGenerator(SecureRandom secureRandom) {
        this.secureRandom = Objects.requireNonNull(secureRandom);
    }

    @Override
    public PasswordRecoverySecret generate() {
        var bytes = new byte[SECRET_BYTES];
        try {
            secureRandom.nextBytes(bytes);
            return new PasswordRecoverySecret(
                    Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
        } finally {
            Arrays.fill(bytes, (byte) 0);
        }
    }
}
