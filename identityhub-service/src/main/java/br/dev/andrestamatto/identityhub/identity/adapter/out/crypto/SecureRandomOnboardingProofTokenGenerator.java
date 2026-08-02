package br.dev.andrestamatto.identityhub.identity.adapter.out.crypto;

import br.dev.andrestamatto.identityhub.identity.application.OnboardingProofTokenGenerator;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

public final class SecureRandomOnboardingProofTokenGenerator
        implements OnboardingProofTokenGenerator {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom;

    public SecureRandomOnboardingProofTokenGenerator(SecureRandom secureRandom) {
        this.secureRandom = Objects.requireNonNull(secureRandom);
    }

    @Override
    public String generate() {
        var bytes = new byte[TOKEN_BYTES];
        try {
            secureRandom.nextBytes(bytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } finally {
            Arrays.fill(bytes, (byte) 0);
        }
    }
}
