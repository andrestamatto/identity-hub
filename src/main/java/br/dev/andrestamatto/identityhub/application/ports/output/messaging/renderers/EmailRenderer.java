package br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.email.EmailContent;

/**
 * Email rendering port.
 * Implementations transform a notification message and its selected template into a rendered email.
 */
public interface EmailRenderer {
    EmailContent render(NotificationMessage notificationMessage);
}
