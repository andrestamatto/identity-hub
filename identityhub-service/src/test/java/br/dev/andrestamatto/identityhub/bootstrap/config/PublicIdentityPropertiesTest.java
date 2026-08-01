package br.dev.andrestamatto.identityhub.bootstrap.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class PublicIdentityPropertiesTest {

    @Test
    void rejectsNonPositiveOrUnboundedProtectionSettings() {
        assertThatThrownBy(() -> new PublicIdentityProperties(
                        true, 0, Duration.ofMinutes(15), 10_000, Duration.ofMillis(750), 2_048))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PublicIdentityProperties(
                        true, 20, Duration.ZERO, 10_000, Duration.ofMillis(750), 2_048))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PublicIdentityProperties(
                        true, 20, Duration.ofMinutes(15), 0, Duration.ofMillis(750), 2_048))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PublicIdentityProperties(
                        true, 20, Duration.ofMinutes(15), 10_000, Duration.ofSeconds(6), 2_048))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PublicIdentityProperties(
                        true, 20, Duration.ofMinutes(15), 10_000, Duration.ofMillis(750), 100))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
