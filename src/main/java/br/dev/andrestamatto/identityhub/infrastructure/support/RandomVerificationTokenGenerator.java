package br.dev.andrestamatto.identityhub.infrastructure.support;

import br.dev.andrestamatto.identityhub.application.ports.output.VerificationTokenGenerator;
import br.dev.andrestamatto.identityhub.domain.valueobjects.NotificationMethod;
import br.dev.andrestamatto.identityhub.domain.valueobjects.VerificationToken;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;

public class RandomVerificationTokenGenerator implements VerificationTokenGenerator {

    private final Clock clock;
    private final Random random;
    private final Duration ttl;

    public RandomVerificationTokenGenerator(Clock clock, Random random, Duration ttl) {
        this.clock = clock;
        this.random = random;
        this.ttl = ttl;
    }

    @Override
    public VerificationToken generate(NotificationMethod method) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        Instant expiresAt = Instant.now(clock).plus(ttl);
        return new VerificationToken(code, method, expiresAt);
    }

}
