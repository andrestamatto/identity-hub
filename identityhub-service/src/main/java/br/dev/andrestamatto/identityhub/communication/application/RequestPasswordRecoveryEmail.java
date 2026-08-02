package br.dev.andrestamatto.identityhub.communication.application;

import br.dev.andrestamatto.identityhub.communication.domain.EmailDeliveryId;
import br.dev.andrestamatto.identityhub.communication.domain.EmailRecipient;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public final class RequestPasswordRecoveryEmail {

    private final EmailDeliveryRepository repository;
    private final EmailOriginResolver originResolver;
    private final Clock clock;

    public RequestPasswordRecoveryEmail(
            EmailDeliveryRepository repository,
            EmailOriginResolver originResolver,
            Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.originResolver = Objects.requireNonNull(originResolver);
        this.clock = Objects.requireNonNull(clock);
    }

    public void execute(Command command) {
        Objects.requireNonNull(command);
        var id = new EmailDeliveryId(command.deliveryId());
        var recipient = new EmailRecipient(command.recipient());
        var existing = repository.find(id);
        if (existing.isPresent()) {
            if (!existing.orElseThrow().matchesPasswordRecovery(
                    command.applicationId(), recipient, command.correlationId())) {
                throw new EmailDeliveryConflictException();
            }
            return;
        }
        repository.add(EmailDelivery.requestPasswordRecovery(
                id,
                originResolver.resolve(command.applicationId()),
                recipient,
                command.recoveryUrl(),
                command.correlationId(),
                clock.instant()));
    }

    public record Command(
            UUID deliveryId,
            UUID applicationId,
            String recipient,
            String recoveryUrl,
            String correlationId) {

        public Command {
            Objects.requireNonNull(deliveryId);
            Objects.requireNonNull(applicationId);
            Objects.requireNonNull(recipient);
            Objects.requireNonNull(recoveryUrl);
            Objects.requireNonNull(correlationId);
        }

        @Override
        public String toString() {
            return "RequestPasswordRecoveryEmail.Command[deliveryId=" + deliveryId
                    + ", content=REDACTED]";
        }
    }
}
