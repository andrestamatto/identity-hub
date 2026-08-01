package br.dev.andrestamatto.identityhub.identity.application;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public final class BeginLocalRegistration {

    private final RegisterPendingLocalIdentity registerIdentity;
    private final RequestEmailVerification requestVerification;

    public BeginLocalRegistration(
            RegisterPendingLocalIdentity registerIdentity,
            RequestEmailVerification requestVerification) {
        this.registerIdentity = Objects.requireNonNull(registerIdentity);
        this.requestVerification = Objects.requireNonNull(requestVerification);
    }

    public Result execute(Command command) {
        Objects.requireNonNull(command);
        try (command) {
            var password = command.passwordCopy();
            try {
                var registration = registerIdentity.execute(
                        new RegisterPendingLocalIdentity.Command(
                                command.applicationId(), command.email(), password));
                var verification = requestVerification.execute(
                        new RequestEmailVerification.Command(
                                command.applicationId(),
                                registration.userAccountRef(),
                                command.email(),
                                command.correlationId()));
                return new Result(
                        registration.userAccountRef(), verification.challengeId());
            } finally {
                Arrays.fill(password, '\0');
            }
        }
    }

    public static final class Command implements AutoCloseable {
        private final UUID applicationId;
        private final String email;
        private final char[] password;
        private final String correlationId;

        public Command(
                UUID applicationId,
                String email,
                char[] password,
                String correlationId) {
            this.applicationId = Objects.requireNonNull(applicationId);
            this.email = Objects.requireNonNull(email);
            this.password = Objects.requireNonNull(password).clone();
            this.correlationId = Objects.requireNonNull(correlationId);
        }

        UUID applicationId() {
            return applicationId;
        }

        String email() {
            return email;
        }

        String correlationId() {
            return correlationId;
        }

        char[] passwordCopy() {
            return password.clone();
        }

        @Override
        public void close() {
            Arrays.fill(password, '\0');
        }

        @Override
        public String toString() {
            return "BeginLocalRegistration.Command[applicationId=" + applicationId
                    + ", credentials=REDACTED]";
        }
    }

    public record Result(UUID userAccountRef, UUID challengeId) { }
}
