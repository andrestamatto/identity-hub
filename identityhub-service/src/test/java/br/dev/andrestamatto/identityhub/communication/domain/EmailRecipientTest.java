package br.dev.andrestamatto.identityhub.communication.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class EmailRecipientTest {

    @Test
    void acceptsAndNormalizesValidAddress() {
        assertThat(new EmailRecipient("  andre@example.com ").value())
                .isEqualTo("andre@example.com");
    }

    @Test
    void rejectsMalformedOrUnsafeAddresses() {
        assertThatIllegalArgumentException().isThrownBy(() -> new EmailRecipient("invalid"));
        assertThatIllegalArgumentException().isThrownBy(() -> new EmailRecipient("a@@example.com"));
        assertThatIllegalArgumentException().isThrownBy(() -> new EmailRecipient("a@example.com\nBcc:x@y.io"));
    }
}
