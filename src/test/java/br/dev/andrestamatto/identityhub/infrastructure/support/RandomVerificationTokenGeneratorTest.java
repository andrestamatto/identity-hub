package br.dev.andrestamatto.identityhub.infrastructure.support;

import br.dev.andrestamatto.identityhub.domain.valueobjects.NotificationMethod;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RandomVerificationTokenGeneratorTest {

    @Test
    public void shouldGenerateSixDigitVerificationTokenWithConfiguredMethodAndExpiration() {
        var now = Instant.parse("2026-06-09T10:00:00Z");
        var clock = Clock.fixed(now, ZoneOffset.UTC);
        var ttl = Duration.ofMinutes(15);
        var generator = new RandomVerificationTokenGenerator(clock, new Random(1), ttl);

        var token = generator.generate(NotificationMethod.EMAIL);

        assertTrue(token.code().matches("\\d{6}"));
        assertEquals(NotificationMethod.EMAIL, token.method());
        assertEquals(now.plus(ttl), token.expiresAt());
    }
}
