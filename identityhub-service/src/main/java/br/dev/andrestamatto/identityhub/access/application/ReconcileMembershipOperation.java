package br.dev.andrestamatto.identityhub.access.application;

import br.dev.andrestamatto.identityhub.access.domain.MembershipApplicationRef;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ReconcileMembershipOperation {

    private final MembershipGrantRepository repository;
    private final Clock clock;

    public ReconcileMembershipOperation(
            MembershipGrantRepository repository,
            Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    public Optional<MembershipOperationStatus> execute(
            UUID operationId,
            UUID applicationId) {
        Objects.requireNonNull(operationId);
        return repository.requeue(
                operationId,
                new MembershipApplicationRef(applicationId),
                clock.instant());
    }
}
