package br.dev.andrestamatto.identityhub.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.andrestamatto.identityhub.identity.domain.UserAccountRef;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DisableGlobalAccountTest {

    private static final Instant NOW = Instant.parse("2026-08-02T18:00:00Z");
    private static final UUID ACCOUNT_ID =
            UUID.fromString("f20ace9e-e02a-436f-93e6-edaff0320733");

    @Test
    void persistsIntentBeforeDisablingAndCompletesTheOperation() {
        var repository = new InMemoryRepository();
        var gateway = new RecordingGateway(repository);
        var useCase = useCase(repository, gateway);

        var result = useCase.execute(command("disable-account-001", "Confirmed security incident"));

        assertThat(gateway.operationWasPending).isTrue();
        assertThat(gateway.lifecycleWasLocked).isTrue();
        assertThat(gateway.disabledAccount).isEqualTo(new UserAccountRef(ACCOUNT_ID));
        assertThat(result.status()).isEqualTo(GlobalAccountDisableStatus.COMPLETED);
        assertThat(result.actorSubject()).isEqualTo("admin-subject");
        assertThat(result.reason()).isEqualTo("Confirmed security incident");
        assertThat(result.correlationId()).isEqualTo("correlation-001");
    }

    @Test
    void equivalentReplayReturnsTheCompletedOperationWithoutAnotherExternalEffect() {
        var repository = new InMemoryRepository();
        var gateway = new RecordingGateway(repository);
        var useCase = useCase(repository, gateway);
        var command = command("disable-account-002", "Account owner requested containment");

        var first = useCase.execute(command);
        var replay = useCase.execute(command);

        assertThat(replay.operationId()).isEqualTo(first.operationId());
        assertThat(gateway.calls).isEqualTo(1);
    }

    @Test
    void sameIdempotencyKeyCannotRepresentAnotherCommand() {
        var repository = new InMemoryRepository();
        var gateway = new RecordingGateway(repository);
        var useCase = useCase(repository, gateway);
        useCase.execute(command("disable-account-003", "Confirmed security incident"));

        assertThatThrownBy(() -> useCase.execute(
                        command("disable-account-003", "Different administrative reason")))
                .isInstanceOf(GlobalAccountDisableConflictException.class);
        assertThat(gateway.calls).isEqualTo(1);
    }

    @Test
    void retryableFailureRemainsDurableAndCanBeRetriedWithTheSameKey() {
        var repository = new InMemoryRepository();
        var gateway = new RecordingGateway(repository);
        gateway.failNext = true;
        var useCase = useCase(repository, gateway);
        var command = command("disable-account-004", "Account owner reported compromise");

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(GlobalAccountDisableUnavailableException.class);
        assertThat(repository.byKey.get("disable-account-004").status())
                .isEqualTo(GlobalAccountDisableStatus.FAILED);

        var retried = useCase.execute(command);

        assertThat(retried.status()).isEqualTo(GlobalAccountDisableStatus.COMPLETED);
        assertThat(gateway.calls).isEqualTo(2);
    }

    @Test
    void permanentRejectionIsRecordedAndNotRetried() {
        var repository = new InMemoryRepository();
        var gateway = new RecordingGateway(repository);
        gateway.rejection = GlobalAccountDisableRejection.LAST_ENABLED_PLATFORM_ADMIN;
        var useCase = useCase(repository, gateway);
        var command = command("disable-account-005", "Administrative account compromised");

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(GlobalAccountDisableRejectedException.class);
        assertThat(repository.byKey.get("disable-account-005").status())
                .isEqualTo(GlobalAccountDisableStatus.REJECTED);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(GlobalAccountDisableRejectedException.class);
        assertThat(gateway.calls).isEqualTo(1);
    }

    private DisableGlobalAccount useCase(
            GlobalAccountDisableOperationRepository repository,
            GlobalAccountDisabler gateway) {
        return new DisableGlobalAccount(
                repository,
                gateway,
                new DirectTransaction(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                () -> UUID.fromString("0658c077-6544-47fc-9755-d7491f07dc5b"));
    }

    private DisableGlobalAccount.Command command(String idempotencyKey, String reason) {
        return new DisableGlobalAccount.Command(
                new UserAccountRef(ACCOUNT_ID),
                reason,
                idempotencyKey,
                "admin-subject",
                "correlation-001");
    }

    private static final class InMemoryRepository
            implements GlobalAccountDisableOperationRepository {
        private final Map<String, GlobalAccountDisableOperation> byKey = new HashMap<>();

        @Override
        public GlobalAccountDisableOperation findByIdempotencyKey(String key) {
            return byKey.get(key);
        }

        @Override
        public void save(GlobalAccountDisableOperation operation) {
            byKey.put(operation.idempotencyKey(), operation);
        }

        @Override
        public void lockGlobalAccountLifecycle() {
            lifecycleLocked = true;
        }

        private boolean lifecycleLocked;
    }

    private static final class RecordingGateway implements GlobalAccountDisabler {
        private final InMemoryRepository repository;
        private UserAccountRef disabledAccount;
        private boolean operationWasPending;
        private boolean lifecycleWasLocked;
        private boolean failNext;
        private GlobalAccountDisableRejection rejection;
        private int calls;

        private RecordingGateway(InMemoryRepository repository) {
            this.repository = repository;
        }

        @Override
        public void disable(UserAccountRef userAccountRef) {
            calls++;
            disabledAccount = userAccountRef;
            operationWasPending = repository.byKey.values().stream()
                    .anyMatch(operation -> operation.status() == GlobalAccountDisableStatus.PENDING);
            lifecycleWasLocked = repository.lifecycleLocked;
            if (rejection != null) {
                throw new GlobalAccountDisableGatewayRejection(rejection);
            }
            if (failNext) {
                failNext = false;
                throw new GlobalAccountDisableGatewayException();
            }
        }
    }

    private static final class DirectTransaction implements IdentityTransaction {
        @Override
        public <T> T execute(java.util.function.Supplier<T> work) {
            return work.get();
        }

        @Override
        public void execute(Runnable work) {
            work.run();
        }
    }
}
