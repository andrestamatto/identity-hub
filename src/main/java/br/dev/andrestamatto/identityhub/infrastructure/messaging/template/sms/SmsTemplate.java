package br.dev.andrestamatto.identityhub.infrastructure.messaging.template.sms;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.SmsMessageTemplate;

/**
 * Plain-text SMS template renderer.
 * Implementations map notification details to short messages accepted by SMS providers.
 */
public interface SmsTemplate {
    SmsMessageTemplate supports();

    String render(NotificationMessage notificationMessage);
}
