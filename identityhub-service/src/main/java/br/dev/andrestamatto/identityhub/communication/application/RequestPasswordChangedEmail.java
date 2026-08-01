package br.dev.andrestamatto.identityhub.communication.application;

import br.dev.andrestamatto.identityhub.communication.domain.EmailDeliveryId;
import br.dev.andrestamatto.identityhub.communication.domain.EmailRecipient;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public final class RequestPasswordChangedEmail {

    private final EmailDeliveryRepository repository;
    private final EmailOriginResolver originResolver;
    private final Clock clock;

    public RequestPasswordChangedEmail(
            EmailDeliveryRepository repository,
            EmailOriginResolver originResolver,
            Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.originResolver = Objects.requireNonNull(originResolver);
        this.clock = Objects.requireNonNull(clock);
    }

    public Result execute(Command command) {
        Objects.requireNonNull(command);
        var id = new EmailDeliveryId(command.deliveryId());
        var recipient = new EmailRecipient(command.recipient());
        var existing = repository.find(id);
        if (existing.isPresent()) {
            if (!existing.orElseThrow().matches(
                    command.applicationId(), recipient, command.correlationId())) {
                throw new EmailDeliveryConflictException();
            }
            return new Result(id.value(), false);
        }
        var delivery = EmailDelivery.request(
                id,
                originResolver.resolve(command.applicationId()),
                recipient,
                EmailDeliveryPurpose.PASSWORD_CHANGED,
                command.correlationId(),
                clock.instant());
        try {
            repository.add(delivery);
            return new Result(id.value(), true);
        } catch (EmailDeliveryConflictException exception) {
            var concurrent = repository.find(id);
            if (concurrent.isPresent()
                    && concurrent.orElseThrow().matches(
                            command.applicationId(), recipient, command.correlationId())) {
                return new Result(id.value(), false);
            }
            throw exception;
        }
    }

    public record Command(
            UUID deliveryId,
            UUID applicationId,
            String recipient,
            String correlationId) {

        public Command {
            Objects.requireNonNull(deliveryId);
            Objects.requireNonNull(applicationId);
        }
    }

    public record Result(UUID deliveryId, boolean created) { }
}
