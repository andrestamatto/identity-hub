package br.dev.andrestamatto.identityhub.identity.adapter.out.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import org.junit.jupiter.api.Test;

class SecureRandomEmailVerificationSecretGeneratorTest {

    @Test
    void generatedSecretsHaveRedactedRepresentation() {
        var generator = new SecureRandomEmailVerificationSecretGenerator(new SecureRandom());

        var first = generator.generate();
        var second = generator.generate();

        assertThat(first.toString()).isEqualTo("EmailVerificationSecret[REDACTED]");
        assertThat(second.toString()).isEqualTo("EmailVerificationSecret[REDACTED]");
        assertThat(first).isNotSameAs(second);
    }
}
