package br.dev.andrestamatto.identityhub.identity.application;

import br.dev.andrestamatto.identityhub.identity.domain.UserAccountRef;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class DisableGlobalAccount {

    private final GlobalAccountDisableOperationRepository repository;
    private final GlobalAccountDisabler disabler;
    private final IdentityTransaction transaction;
    private final Clock clock;
    private final Supplier<UUID> identifiers;

    public DisableGlobalAccount(
            GlobalAccountDisableOperationRepository repository,
            GlobalAccountDisabler disabler,
            IdentityTransaction transaction,
            Clock clock,
            Supplier<UUID> identifiers) {
        this.repository = Objects.requireNonNull(repository);
        this.disabler = Objects.requireNonNull(disabler);
        this.transaction = Objects.requireNonNull(transaction);
        this.clock = Objects.requireNonNull(clock);
        this.identifiers = Objects.requireNonNull(identifiers);
    }

    public GlobalAccountDisableOperation execute(Command command) {
        Objects.requireNonNull(command);
        var fingerprint = fingerprint(command.userAccountRef(), command.reason());
        var operation = transaction.execute(() -> findOrCreate(command, fingerprint));
        ensureEquivalent(operation, fingerprint);
        if (operation.status() == GlobalAccountDisableStatus.COMPLETED) {
            return operation;
        }
        if (operation.status() == GlobalAccountDisableStatus.REJECTED) {
            throw new GlobalAccountDisableRejectedException(operation.rejection());
        }
        if (operation.status() == GlobalAccountDisableStatus.FAILED) {
            operation = operation.pending();
            var pending = operation;
            transaction.execute(() -> repository.save(pending));
        }
        return apply(operation);
    }

    private GlobalAccountDisableOperation findOrCreate(Command command, String fingerprint) {
        var existing = repository.findByIdempotencyKey(command.idempotencyKey());
        if (existing != null) {
            return existing;
        }
        var created = new GlobalAccountDisableOperation(
                identifiers.get(),
                command.userAccountRef(),
                command.reason(),
                command.idempotencyKey(),
                fingerprint,
                command.actorSubject(),
                command.correlationId(),
                GlobalAccountDisableStatus.PENDING,
                null,
                clock.instant(),
                null);
        repository.save(created);
        return repository.findByIdempotencyKey(command.idempotencyKey());
    }

    private GlobalAccountDisableOperation apply(GlobalAccountDisableOperation operation) {
        var outcome = transaction.execute(() -> applySerialized(operation));
        if (outcome.failure() != null) {
            throw outcome.failure();
        }
        return outcome.operation();
    }

    private ApplyOutcome applySerialized(GlobalAccountDisableOperation operation) {
        repository.lockGlobalAccountLifecycle();
        try {
            disabler.disable(operation.userAccountRef());
            var completed = operation.completed(clock.instant());
            repository.save(completed);
            return new ApplyOutcome(completed, null);
        } catch (GlobalAccountDisableGatewayRejection exception) {
            var rejected = operation.rejected(exception.rejection(), clock.instant());
            repository.save(rejected);
            return new ApplyOutcome(
                    rejected,
                    new GlobalAccountDisableRejectedException(exception.rejection()));
        } catch (GlobalAccountDisableGatewayException exception) {
            var failed = operation.failed(clock.instant());
            repository.save(failed);
            return new ApplyOutcome(failed, new GlobalAccountDisableUnavailableException());
        }
    }

    private void ensureEquivalent(
            GlobalAccountDisableOperation operation,
            String fingerprint) {
        if (!MessageDigest.isEqual(
                operation.commandFingerprint().getBytes(StandardCharsets.US_ASCII),
                fingerprint.getBytes(StandardCharsets.US_ASCII))) {
            throw new GlobalAccountDisableConflictException();
        }
    }

    private String fingerprint(UserAccountRef userAccountRef, String reason) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(
                    (userAccountRef.value() + "\n" + reason).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record Command(
            UserAccountRef userAccountRef,
            String reason,
            String idempotencyKey,
            String actorSubject,
            String correlationId) {

        public Command {
            Objects.requireNonNull(userAccountRef);
            reason = requireText(reason, "Reason", 10, 500);
            idempotencyKey = requireText(idempotencyKey, "Idempotency key", 8, 128);
            actorSubject = requireText(actorSubject, "Actor subject", 1, 255);
            correlationId = requireText(correlationId, "Correlation id", 1, 64);
        }

        private static String requireText(
                String value,
                String name,
                int minimum,
                int maximum) {
            Objects.requireNonNull(value);
            var trimmed = value.trim();
            if (trimmed.length() < minimum || trimmed.length() > maximum) {
                throw new IllegalArgumentException(
                        name + " must contain between " + minimum + " and " + maximum
                                + " characters");
            }
            return trimmed;
        }
    }

    private record ApplyOutcome(
            GlobalAccountDisableOperation operation,
            RuntimeException failure) {
    }
}
