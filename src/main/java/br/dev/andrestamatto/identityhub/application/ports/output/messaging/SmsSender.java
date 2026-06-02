package br.dev.andrestamatto.identityhub.application.ports.output.messaging;

public interface SmsSender {
    void send(NotificationMessage notificationMessage);
}
