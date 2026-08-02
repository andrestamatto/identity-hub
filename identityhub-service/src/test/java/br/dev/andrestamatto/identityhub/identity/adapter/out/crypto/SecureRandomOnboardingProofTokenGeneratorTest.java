package br.dev.andrestamatto.identityhub.identity.adapter.out.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import org.junit.jupiter.api.Test;

class SecureRandomOnboardingProofTokenGeneratorTest {

    @Test
    void generatesIndependentBase64UrlTokensWithTwoHundredFiftySixBits() {
        var generator = new SecureRandomOnboardingProofTokenGenerator(new SecureRandom());

        var first = generator.generate();
        var second = generator.generate();

        assertThat(first).matches("[A-Za-z0-9_-]{43}");
        assertThat(second).matches("[A-Za-z0-9_-]{43}").isNotEqualTo(first);
    }
}
