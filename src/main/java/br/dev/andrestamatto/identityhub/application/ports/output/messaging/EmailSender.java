package br.dev.andrestamatto.identityhub.application.ports.output.messaging;

public interface EmailSender {
    void send(NotificationMessage notificationMessage);
}
