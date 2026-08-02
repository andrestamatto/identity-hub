package br.dev.andrestamatto.identityhub.identity.application;

import java.util.Objects;
import java.util.UUID;

public interface RecoveryEmailRequester {

    void request(Command command);

    record Command(
            UUID challengeId,
            UUID applicationId,
            String recipient,
            String recoveryUrl,
            String correlationId) {

        public Command {
            Objects.requireNonNull(challengeId);
            Objects.requireNonNull(applicationId);
            Objects.requireNonNull(recipient);
            Objects.requireNonNull(recoveryUrl);
            Objects.requireNonNull(correlationId);
        }

        @Override
        public String toString() {
            return "RecoveryEmailRequester.Command[challengeId=" + challengeId
                    + ", content=REDACTED]";
        }
    }
}
