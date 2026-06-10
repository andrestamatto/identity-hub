package br.dev.andrestamatto.identityhub.infrastructure.messaging;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.senders.SmsSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Placeholder SMS sender for the current notification flow.
 * It makes SMS routing explicit while real provider integration is still pending.
 */
public class UserVerificationSmsSender implements SmsSender {
    private static final Logger log = LoggerFactory.getLogger(UserVerificationSmsSender.class);

    @Override
    public void send(NotificationMessage notificationMessage) {
        log.warn("SMS notification is not implemented yet. Recipient={}", notificationMessage.recipient());
    }
}
