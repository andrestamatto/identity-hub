package br.dev.andrestamatto.identityhub.identity.adapter.out.crypto;

import br.dev.andrestamatto.identityhub.identity.application.OnboardingSessionIdGenerator;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

public final class SecureRandomOnboardingSessionIdGenerator
        implements OnboardingSessionIdGenerator {

    private static final int RANDOM_BYTES = 32;

    private final SecureRandom secureRandom;

    public SecureRandomOnboardingSessionIdGenerator(SecureRandom secureRandom) {
        this.secureRandom = Objects.requireNonNull(secureRandom);
    }

    @Override
    public String generate() {
        var bytes = new byte[RANDOM_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
