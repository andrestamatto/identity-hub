package br.dev.andrestamatto.identityhub.application.ports.output.messaging.senders;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;

/**
 * SMS sender port for notification messages.
 * The concrete provider is intentionally hidden behind infrastructure configuration.
 */
public interface SmsSender {
    void send(NotificationMessage notificationMessage);
}
