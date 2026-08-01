package br.dev.andrestamatto.identityhub.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class LocalPasswordTest {

    @Test
    void acceptsLongUnicodePassphraseWithoutCompositionRules() {
        try (var password = new LocalPassword("frase longa com café seguro".toCharArray())) {
            assertThat(password.copy()).containsExactly(
                    "frase longa com café seguro".toCharArray());
            assertThat(password.toString()).isEqualTo("LocalPassword[REDACTED]");
        }
    }

    @Test
    void rejectsShortOrCommonPasswords() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> new LocalPassword("short".toCharArray()));
        assertThatIllegalArgumentException().isThrownBy(
                () -> new LocalPassword("passwordpassword".toCharArray()));
    }

    @Test
    void clearsInternalValueWhenClosed() {
        var password = new LocalPassword("frase longa e segura".toCharArray());
        password.close();

        assertThat(password.copy()).containsOnly('\0');
    }
}
