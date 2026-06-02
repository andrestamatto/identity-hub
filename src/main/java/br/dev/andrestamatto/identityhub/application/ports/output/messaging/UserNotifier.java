package br.dev.andrestamatto.identityhub.application.ports.output.messaging;

import br.dev.andrestamatto.identityhub.domain.valueobjects.NotificationMethod;

public interface UserNotifier {
    void notify(NotificationMessage notificationMessage, NotificationMethod method);
}
