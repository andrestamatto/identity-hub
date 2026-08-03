package br.dev.andrestamatto.identityhub.access.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.andrestamatto.identityhub.access.domain.Membership;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GrantMembershipTest {

    private static final UUID APPLICATION_ID =
            UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
    private static final UUID APPLICATION_CLIENT_ID =
            UUID.fromString("72c43df3-9f34-4dc6-85cc-5d323762f299");
    private static final UUID USER_ACCOUNT_REF =
            UUID.fromString("680ac2e4-bfb0-4375-a75e-453b6e7b600c");
    private static final UUID MEMBERSHIP_ID =
            UUID.fromString("c50638fe-0b91-4f47-81e6-2bd183040c1c");
    private static final UUID OPERATION_ID =
            UUID.fromString("06d068e0-78d5-4df6-97dd-88df538ab948");
    private static final Instant NOW = Instant.parse("2026-08-02T18:00:00Z");

    private final InMemoryRepository repository = new InMemoryRepository();
    private final GrantMembership grant = new GrantMembership(
            repository,
            Clock.fixed(NOW, ZoneOffset.UTC),
            new SequenceIdentifiers());

    @Test
    void createsOnePendingMembershipForTheAuthorizedApplication() {
        var result = grant.execute(command(USER_ACCOUNT_REF, "membership-grant-001"));

        assertThat(result.operationId()).isEqualTo(OPERATION_ID);
        assertThat(result.membershipId()).isEqualTo(MEMBERSHIP_ID);
        assertThat(result.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(result.userAccountRef()).isEqualTo(USER_ACCOUNT_REF);
        assertThat(result.state()).isEqualTo("PENDING");
        assertThat(repository.operations).hasSize(1);
    }

    @Test
    void replaysTheSameOperationAndDoesNotDuplicateMembership() {
        var first = grant.execute(command(USER_ACCOUNT_REF, "membership-grant-001"));
        var replay = grant.execute(command(USER_ACCOUNT_REF, "membership-grant-001"));

        assertThat(replay).isEqualTo(first);
        assertThat(repository.memberships).hasSize(1);
    }

    @Test
    void rejectsReuseOfIdempotencyKeyForAnotherCommand() {
        grant.execute(command(USER_ACCOUNT_REF, "membership-grant-001"));

        assertThatThrownBy(() -> grant.execute(command(
                        UUID.fromString("e99f1179-4324-4a4c-b5bc-a7b2151d037d"),
                        "membership-grant-001")))
                .isInstanceOf(MembershipGrantConflictException.class);
        assertThat(repository.memberships).hasSize(1);
    }

    private GrantMembership.Command command(UUID userAccountRef, String idempotencyKey) {
        return new GrantMembership.Command(
                APPLICATION_ID,
                APPLICATION_CLIENT_ID,
                userAccountRef,
                idempotencyKey,
                "grant-membership");
    }

    private static final class SequenceIdentifiers implements java.util.function.Supplier<UUID> {
        private int index;

        @Override
        public UUID get() {
            return switch (index++) {
                case 0 -> MEMBERSHIP_ID;
                case 1 -> OPERATION_ID;
                default -> UUID.randomUUID();
            };
        }
    }

    private static final class InMemoryRepository implements MembershipGrantRepository {
        private final Map<String, MembershipGrantOperation> operations = new HashMap<>();
        private final Map<String, Membership> memberships = new HashMap<>();

        @Override
        public MembershipGrantOperation addOrReplay(MembershipGrantOperation proposed) {
            var existingOperation = operations.get(proposed.idempotencyKey());
            if (existingOperation != null) {
                return existingOperation;
            }
            var membershipKey = proposed.membership().applicationRef().value()
                    + ":" + proposed.membership().userAccountRef().value();
            var membership = memberships.computeIfAbsent(
                    membershipKey, ignored -> proposed.membership());
            var stored = proposed.withMembership(membership);
            operations.put(stored.idempotencyKey(), stored);
            return stored;
        }

        @Override
        public java.util.Optional<MembershipOperationStatus> findStatus(
                java.util.UUID operationId,
                br.dev.andrestamatto.identityhub.access.domain.MembershipApplicationRef
                        applicationRef) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<MembershipOperationStatus> requeue(
                java.util.UUID operationId,
                br.dev.andrestamatto.identityhub.access.domain.MembershipApplicationRef
                        applicationRef,
                java.time.Instant now) {
            return java.util.Optional.empty();
        }
    }
}
