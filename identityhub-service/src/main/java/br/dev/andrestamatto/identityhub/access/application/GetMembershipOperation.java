package br.dev.andrestamatto.identityhub.access.application;

import br.dev.andrestamatto.identityhub.access.domain.MembershipApplicationRef;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class GetMembershipOperation {

    private final MembershipGrantRepository repository;

    public GetMembershipOperation(MembershipGrantRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public Optional<MembershipOperationStatus> execute(
            UUID operationId,
            UUID applicationId) {
        Objects.requireNonNull(operationId);
        return repository.findStatus(
                operationId,
                new MembershipApplicationRef(applicationId));
    }
}
