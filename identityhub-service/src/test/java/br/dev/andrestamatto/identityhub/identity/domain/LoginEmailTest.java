package br.dev.andrestamatto.identityhub.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class LoginEmailTest {

    @Test
    void preservesContactFormAndNormalizesIdentity() {
        var email = new LoginEmail("  Andre@ExAmPle.COM  ");

        assertThat(email.contactValue()).isEqualTo("Andre@ExAmPle.COM");
        assertThat(email.normalizedValue()).isEqualTo("andre@example.com");
    }

    @Test
    void rejectsMalformedOrUnsafeInput() {
        assertThatIllegalArgumentException().isThrownBy(() -> new LoginEmail("invalid"));
        assertThatIllegalArgumentException().isThrownBy(() -> new LoginEmail("a@@example.com"));
        assertThatIllegalArgumentException().isThrownBy(
                () -> new LoginEmail("a@example.com\nBcc:x@y.io"));
    }
}
