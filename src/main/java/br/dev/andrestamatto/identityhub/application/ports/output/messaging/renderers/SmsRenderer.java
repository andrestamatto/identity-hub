package br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.sms.RenderedSms;

/**
 * Renders a NotificationMessage into plain-text SMS content.
 * Provider-specific delivery remains outside this contract.
 */
public interface SmsRenderer {
    RenderedSms render(NotificationMessage notificationMessage);
}
