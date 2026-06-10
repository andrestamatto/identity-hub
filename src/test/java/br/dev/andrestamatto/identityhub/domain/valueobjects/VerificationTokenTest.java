package br.dev.andrestamatto.identityhub.domain.valueobjects;

import br.dev.andrestamatto.identityhub.domain.exceptions.VerificationTokenException;
import br.dev.andrestamatto.identityhub.support.UserTestData;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class VerificationTokenTest {

    @Test
    public void shouldCreateExpiredVerificationTokenForPersistenceRehydration() {
        assertDoesNotThrow(() -> new VerificationToken(
                UserTestData.validVerificationCode,
                NotificationMethod.EMAIL,
                Instant.parse("2026-01-01T10:00:00Z")
        ));
    }

    @Test
    public void shouldRejectExpiredVerificationTokenWhenValidatingCode() {
        var verificationToken = new VerificationToken(
                UserTestData.validVerificationCode,
                NotificationMethod.EMAIL,
                Instant.parse("2026-01-01T10:00:00Z")
        );

        assertThrows(VerificationTokenException.class, () ->
                VerificationToken.validateCode(
                        verificationToken,
                        "123456",
                        Instant.parse("2026-01-01T10:15:01Z")
                )
        );
    }
}
