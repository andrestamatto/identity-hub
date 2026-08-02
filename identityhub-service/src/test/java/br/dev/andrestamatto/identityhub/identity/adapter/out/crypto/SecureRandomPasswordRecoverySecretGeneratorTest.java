package br.dev.andrestamatto.identityhub.identity.adapter.out.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import org.junit.jupiter.api.Test;

class SecureRandomPasswordRecoverySecretGeneratorTest {

    @Test
    void generatesIndependentRedactedSecretsFromThirtyTwoRandomBytes() {
        var generator = new SecureRandomPasswordRecoverySecretGenerator(new SecureRandom());

        var first = generator.generate();
        var second = generator.generate();

        assertThat(first.toString()).isEqualTo("PasswordRecoverySecret[REDACTED]");
        assertThat(second.toString()).isEqualTo("PasswordRecoverySecret[REDACTED]");
        assertThat(first).isNotSameAs(second);
    }
}
