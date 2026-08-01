package br.dev.andrestamatto.identityhub.communication.application;

import br.dev.andrestamatto.identityhub.communication.domain.EmailDeliveryId;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public final class RequeueEmailDelivery {

    private final EmailDeliveryRepository repository;
    private final Clock clock;

    public RequeueEmailDelivery(EmailDeliveryRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    public EmailDelivery execute(UUID deliveryId) {
        return repository.requeue(new EmailDeliveryId(deliveryId), clock.instant())
                .orElseThrow(() -> new EmailDeliveryNotFoundException(deliveryId));
    }
}
