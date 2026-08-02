package br.dev.andrestamatto.identityhub.identity.application;

import br.dev.andrestamatto.identityhub.identity.domain.LocalPassword;
import br.dev.andrestamatto.identityhub.identity.domain.PasswordRecoveryChallenge;
import br.dev.andrestamatto.identityhub.identity.domain.PasswordRecoveryDecision;
import java.time.Clock;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class CompletePasswordRecovery {

    private final PasswordRecoveryChallengeRepository repository;
    private final LocalPasswordResetter resetter;
    private final PasswordChangedNotifier notifier;
    private final VerificationTransaction transaction;
    private final Clock clock;
    private final Supplier<UUID> idGenerator;

    public CompletePasswordRecovery(
            PasswordRecoveryChallengeRepository repository,
            LocalPasswordResetter resetter,
            PasswordChangedNotifier notifier,
            VerificationTransaction transaction,
            Clock clock,
            Supplier<UUID> idGenerator) {
        this.repository = Objects.requireNonNull(repository);
        this.resetter = Objects.requireNonNull(resetter);
        this.notifier = Objects.requireNonNull(notifier);
        this.transaction = Objects.requireNonNull(transaction);
        this.clock = Objects.requireNonNull(clock);
        this.idGenerator = Objects.requireNonNull(idGenerator);
    }

    public void execute(Command command) {
        Objects.requireNonNull(command);
        try (command) {
            try (var password = password(command)) {
                var challenge = consume(command.token());
                resetter.reset(challenge.userAccountRef(), challenge.email(), password);
                notifier.notify(new PasswordChangedNotifier.Command(
                        idGenerator.get(),
                        challenge.applicationId(),
                        challenge.email().contactValue(),
                        command.correlationId()));
            }
        }
    }

    private LocalPassword password(Command command) {
        var value = command.passwordCopy();
        try {
            return new LocalPassword(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidPasswordRecoveryPasswordException();
        } finally {
            Arrays.fill(value, '\0');
        }
    }

    private PasswordRecoveryChallenge consume(String token) {
        var parsed = ParsedToken.from(token);
        var digest = PasswordRecoveryDigest.from(parsed.secret().value());
        var decision = new AtomicReference<>(PasswordRecoveryDecision.INACTIVE);
        var accepted = new AtomicReference<PasswordRecoveryChallenge>();
        try {
            transaction.execute(() -> repository.findForUpdate(parsed.challengeId())
                    .ifPresent(challenge -> {
                        var current = challenge.validate(digest, clock.instant());
                        decision.set(current);
                        if (current == PasswordRecoveryDecision.VALID) {
                            challenge.markUsed(clock.instant());
                            accepted.set(challenge);
                        }
                        if (current != PasswordRecoveryDecision.INACTIVE) {
                            repository.update(challenge);
                        }
                    }));
        } finally {
            Arrays.fill(digest, (byte) 0);
        }
        if (decision.get() != PasswordRecoveryDecision.VALID) {
            throw new PasswordRecoveryRejectedException();
        }
        return accepted.get();
    }

    private record ParsedToken(UUID challengeId, PasswordRecoverySecret secret) {

        private static ParsedToken from(String token) {
            if (token == null) {
                throw new PasswordRecoveryRejectedException();
            }
            var separator = token.indexOf('.');
            if (separator < 1 || separator != token.lastIndexOf('.')
                    || separator == token.length() - 1) {
                throw new PasswordRecoveryRejectedException();
            }
            try {
                return new ParsedToken(
                        UUID.fromString(token.substring(0, separator)),
                        new PasswordRecoverySecret(token.substring(separator + 1)));
            } catch (IllegalArgumentException exception) {
                throw new PasswordRecoveryRejectedException();
            }
        }
    }

    public static final class Command implements AutoCloseable {
        private final String token;
        private final char[] password;
        private final String correlationId;

        public Command(String token, char[] password, String correlationId) {
            this.token = Objects.requireNonNull(token);
            this.password = Objects.requireNonNull(password).clone();
            this.correlationId = Objects.requireNonNull(correlationId);
        }

        String token() {
            return token;
        }

        char[] passwordCopy() {
            return password.clone();
        }

        String correlationId() {
            return correlationId;
        }

        @Override
        public void close() {
            Arrays.fill(password, '\0');
        }

        @Override
        public String toString() {
            return "CompletePasswordRecovery.Command[credentials=REDACTED]";
        }
    }
}
