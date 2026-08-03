package br.dev.andrestamatto.identityhub.access.application;

import br.dev.andrestamatto.identityhub.access.domain.Membership;
import br.dev.andrestamatto.identityhub.access.domain.MembershipApplicationRef;
import br.dev.andrestamatto.identityhub.access.domain.MembershipId;
import br.dev.andrestamatto.identityhub.access.domain.MembershipUserAccountRef;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class GrantMembership {

    private final MembershipGrantRepository repository;
    private final Clock clock;
    private final Supplier<UUID> identifiers;

    public GrantMembership(
            MembershipGrantRepository repository,
            Clock clock,
            Supplier<UUID> identifiers) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
        this.identifiers = Objects.requireNonNull(identifiers);
    }

    public MembershipGrantResult execute(Command command) {
        Objects.requireNonNull(command);
        var fingerprint = fingerprint(command);
        var membership = Membership.request(
                new MembershipId(identifiers.get()),
                new MembershipApplicationRef(command.applicationId()),
                new MembershipUserAccountRef(command.userAccountRef()),
                clock);
        var proposed = new MembershipGrantOperation(
                identifiers.get(),
                membership,
                command.applicationClientId(),
                command.idempotencyKey(),
                fingerprint,
                command.correlationId(),
                membership.requestedAt());
        var stored = repository.addOrReplay(proposed);
        if (!MessageDigest.isEqual(
                stored.commandFingerprint().getBytes(StandardCharsets.US_ASCII),
                fingerprint.getBytes(StandardCharsets.US_ASCII))) {
            throw new MembershipGrantConflictException();
        }
        return MembershipGrantResult.from(stored);
    }

    private String fingerprint(Command command) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var content = command.applicationId() + "\n"
                    + command.applicationClientId() + "\n"
                    + command.userAccountRef();
            return HexFormat.of().formatHex(
                    digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record Command(
            UUID applicationId,
            UUID applicationClientId,
            UUID userAccountRef,
            String idempotencyKey,
            String correlationId) {

        public Command {
            Objects.requireNonNull(applicationId);
            Objects.requireNonNull(applicationClientId);
            Objects.requireNonNull(userAccountRef);
            idempotencyKey = requireText(idempotencyKey, "Idempotency key", 8, 128);
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
}
