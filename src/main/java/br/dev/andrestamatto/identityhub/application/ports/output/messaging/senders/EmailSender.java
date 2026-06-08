package br.dev.andrestamatto.identityhub.application.ports.output.messaging.senders;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;

/**
 * Email sender port for notification messages.
 * Implementations may render templates before delegating to a lower-level EmailDelivery.
 */
public interface EmailSender {
    void send(NotificationMessage notificationMessage);
}
