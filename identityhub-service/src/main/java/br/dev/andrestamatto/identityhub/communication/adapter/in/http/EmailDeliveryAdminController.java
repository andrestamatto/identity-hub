package br.dev.andrestamatto.identityhub.communication.adapter.in.http;

import br.dev.andrestamatto.identityhub.communication.application.EmailDelivery;
import br.dev.andrestamatto.identityhub.communication.application.GetEmailDelivery;
import br.dev.andrestamatto.identityhub.communication.application.RequeueEmailDelivery;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/admin/communication/email-deliveries")
final class EmailDeliveryAdminController {

    private final GetEmailDelivery getEmailDelivery;
    private final RequeueEmailDelivery requeueEmailDelivery;

    EmailDeliveryAdminController(
            GetEmailDelivery getEmailDelivery,
            RequeueEmailDelivery requeueEmailDelivery) {
        this.getEmailDelivery = getEmailDelivery;
        this.requeueEmailDelivery = requeueEmailDelivery;
    }

    @GetMapping("/{deliveryId}")
    EmailDeliveryResponse get(@PathVariable UUID deliveryId) {
        return EmailDeliveryResponse.from(getEmailDelivery.execute(deliveryId));
    }

    @PostMapping("/{deliveryId}/reprocess")
    ResponseEntity<EmailDeliveryResponse> reprocess(@PathVariable UUID deliveryId) {
        return ResponseEntity.accepted()
                .body(EmailDeliveryResponse.from(requeueEmailDelivery.execute(deliveryId)));
    }

    record EmailDeliveryResponse(
            UUID deliveryId,
            UUID applicationId,
            String applicationIdentifier,
            String environment,
            String purpose,
            String state,
            int attempts,
            Instant nextAttemptAt,
            String lastFailureCode,
            Instant requestedAt,
            Instant updatedAt) {

        static EmailDeliveryResponse from(EmailDelivery delivery) {
            return new EmailDeliveryResponse(
                    delivery.id().value(),
                    delivery.applicationId(),
                    delivery.applicationIdentifier(),
                    delivery.environment(),
                    delivery.purpose().name(),
                    delivery.state().name(),
                    delivery.attempts(),
                    delivery.nextAttemptAt(),
                    delivery.lastFailureCode(),
                    delivery.requestedAt(),
                    delivery.updatedAt());
        }
    }
}
