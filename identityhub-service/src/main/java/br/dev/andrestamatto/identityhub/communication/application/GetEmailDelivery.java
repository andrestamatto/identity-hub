package br.dev.andrestamatto.identityhub.communication.application;

import br.dev.andrestamatto.identityhub.communication.domain.EmailDeliveryId;
import java.util.Objects;
import java.util.UUID;

public final class GetEmailDelivery {

    private final EmailDeliveryRepository repository;

    public GetEmailDelivery(EmailDeliveryRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public EmailDelivery execute(UUID deliveryId) {
        return repository.find(new EmailDeliveryId(deliveryId))
                .orElseThrow(() -> new EmailDeliveryNotFoundException(deliveryId));
    }
}
