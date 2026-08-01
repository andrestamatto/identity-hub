package br.dev.andrestamatto.identityhub.identity.application;

import br.dev.andrestamatto.identityhub.identity.domain.LocalPassword;
import br.dev.andrestamatto.identityhub.identity.domain.LoginEmail;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public final class RegisterPendingLocalIdentity {

    private final SelfRegistrationPolicyResolver policyResolver;
    private final LocalIdentityRegistrar registrar;

    public RegisterPendingLocalIdentity(
            SelfRegistrationPolicyResolver policyResolver,
            LocalIdentityRegistrar registrar) {
        this.policyResolver = Objects.requireNonNull(policyResolver);
        this.registrar = Objects.requireNonNull(registrar);
    }

    public Result execute(Command command) {
        Objects.requireNonNull(command);
        try (command) {
            var email = new LoginEmail(command.email());
            var passwordValue = command.passwordCopy();
            try (var password = new LocalPassword(passwordValue)) {
                if (!policyResolver.isEnabled(command.applicationId())) {
                    throw new SelfRegistrationDisabledException();
                }
                var registration = registrar.register(
                        new PendingLocalIdentity(email, password));
                return new Result(
                        registration.userAccountRef().value(),
                        registration.created());
            } finally {
                Arrays.fill(passwordValue, '\0');
            }
        }
    }

    public static final class Command implements AutoCloseable {
        private final UUID applicationId;
        private final String email;
        private final char[] password;

        public Command(UUID applicationId, String email, char[] password) {
            this.applicationId = Objects.requireNonNull(applicationId);
            this.email = Objects.requireNonNull(email);
            this.password = Objects.requireNonNull(password).clone();
        }

        public UUID applicationId() {
            return applicationId;
        }

        public String email() {
            return email;
        }

        private char[] passwordCopy() {
            return password.clone();
        }

        @Override
        public void close() {
            Arrays.fill(password, '\0');
        }

        @Override
        public String toString() {
            return "RegisterPendingLocalIdentity.Command[applicationId="
                    + applicationId + ", credentials=REDACTED]";
        }
    }

    public record Result(UUID userAccountRef, boolean created) { }
}
