package br.dev.andrestamatto.identityhub.application.ports.output.messaging.notifiers;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.domain.valueobjects.NotificationMethod;

/**
 * Application output port used to notify users after domain/application events.
 * Implementations route the message to the appropriate channel senders.
 */
public interface UserNotifier {
    void notify(NotificationMessage notificationMessage, NotificationMethod method);
}
