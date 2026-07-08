package br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.sms.SmsContent;

/**
 * Renders a NotificationMessage into plain-text SMS content.
 * Provider-specific delivery remains outside this contract.
 */
public interface SmsRenderer {
    SmsContent render(NotificationMessage notificationMessage);
}
