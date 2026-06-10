package br.dev.andrestamatto.identityhub.infrastructure.messaging.templates;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.EmailMessageTemplate;

/**
 * Contract for infrastructure email templates.
 * Implementations declare the template key they support and render a NotificationMessage
 * into an email body ready for delivery.
 */
public interface EmailTemplate {
    EmailMessageTemplate supports();
    String render(NotificationMessage notificationMessage);
}
