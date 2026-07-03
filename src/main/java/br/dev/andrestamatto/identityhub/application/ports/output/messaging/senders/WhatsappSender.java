package br.dev.andrestamatto.identityhub.application.ports.output.messaging.senders;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;

public interface WhatsappSender {
    void send(NotificationMessage notificationMessage);
}
