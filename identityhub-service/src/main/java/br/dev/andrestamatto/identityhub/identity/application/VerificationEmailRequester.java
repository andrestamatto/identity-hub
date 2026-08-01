package br.dev.andrestamatto.identityhub.identity.application;

import java.util.Objects;
import java.util.UUID;

@FunctionalInterface
public interface VerificationEmailRequester {

    void request(Command command);

    record Command(
            UUID deliveryId,
            UUID applicationId,
            String recipient,
            String verificationUrl,
            String correlationId) {

        public Command {
            Objects.requireNonNull(deliveryId);
            Objects.requireNonNull(applicationId);
            Objects.requireNonNull(recipient);
            Objects.requireNonNull(verificationUrl);
            Objects.requireNonNull(correlationId);
        }

        @Override
        public String toString() {
            return "VerificationEmailRequester.Command[deliveryId=" + deliveryId
                    + ", content=REDACTED]";
        }
    }
}
