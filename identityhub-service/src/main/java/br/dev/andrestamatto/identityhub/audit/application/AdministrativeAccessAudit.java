package br.dev.andrestamatto.identityhub.audit.application;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class AdministrativeAccessAudit {

    private final AdministrativeAccessEventRepository repository;
    private final Clock clock;
    private final Supplier<UUID> identifierGenerator;

    public AdministrativeAccessAudit(
            AdministrativeAccessEventRepository repository,
            Clock clock,
            Supplier<UUID> identifierGenerator) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
        this.identifierGenerator = Objects.requireNonNull(identifierGenerator);
    }

    public void record(AdministrativeAccessAttempt attempt) {
        repository.append(new AdministrativeAccessEvent(
                identifierGenerator.get(),
                clock.instant(),
                attempt.correlationId(),
                attempt.actorSubject(),
                attempt.method(),
                attempt.path(),
                attempt.outcome(),
                attempt.reason()));
    }
}
