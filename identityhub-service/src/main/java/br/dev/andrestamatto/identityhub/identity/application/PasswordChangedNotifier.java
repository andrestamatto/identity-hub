package br.dev.andrestamatto.identityhub.identity.application;

import java.util.Objects;
import java.util.UUID;

public interface PasswordChangedNotifier {

    void notify(Command command);

    record Command(
            UUID deliveryId,
            UUID applicationId,
            String recipient,
            String correlationId) {

        public Command {
            Objects.requireNonNull(deliveryId);
            Objects.requireNonNull(applicationId);
            Objects.requireNonNull(recipient);
            Objects.requireNonNull(correlationId);
        }

        @Override
        public String toString() {
            return "PasswordChangedNotifier.Command[deliveryId=" + deliveryId
                    + ", recipient=REDACTED]";
        }
    }
}
