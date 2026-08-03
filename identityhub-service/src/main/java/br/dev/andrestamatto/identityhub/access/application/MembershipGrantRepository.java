package br.dev.andrestamatto.identityhub.access.application;

import br.dev.andrestamatto.identityhub.access.domain.MembershipApplicationRef;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface MembershipGrantRepository {

    MembershipGrantOperation addOrReplay(MembershipGrantOperation proposed);

    Optional<MembershipOperationStatus> findStatus(
            UUID operationId,
            MembershipApplicationRef applicationRef);

    Optional<MembershipOperationStatus> requeue(
            UUID operationId,
            MembershipApplicationRef applicationRef,
            Instant now);
}
