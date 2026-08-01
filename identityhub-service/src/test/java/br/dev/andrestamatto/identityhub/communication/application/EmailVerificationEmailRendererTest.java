package br.dev.andrestamatto.identityhub.communication.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.dev.andrestamatto.identityhub.communication.domain.EmailDeliveryId;
import br.dev.andrestamatto.identityhub.communication.domain.EmailRecipient;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EmailVerificationEmailRendererTest {

    @Test
    void rendersOfficialLinkWithoutExposingItThroughObjectStrings() {
        var verificationUrl = "https://auth.dev.example.test/verify-email?token="
                + "27f3aa0b-6a70-43bd-a087-d5bc0c1bc779.test-only-verification-secret";
        var delivery = EmailDelivery.requestVerification(
                new EmailDeliveryId(
                        UUID.fromString("27f3aa0b-6a70-43bd-a087-d5bc0c1bc779")),
                new EmailOrigin(
                        UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0"),
                        "auto-radar",
                        "Auto Radar",
                        "development"),
                new EmailRecipient("andre@example.test"),
                verificationUrl,
                "verification-renderer",
                Instant.parse("2026-07-31T18:00:00Z"));

        var email = new EmailVerificationEmailRenderer().render(delivery);

        assertThat(email.subject()).isEqualTo("[Auto Radar] Verify your email");
        assertThat(email.body()).contains(verificationUrl).contains("30 minutes");
        assertThat(email.toString()).doesNotContain(verificationUrl);
        assertThat(delivery.toString()).doesNotContain(verificationUrl);
    }
}
