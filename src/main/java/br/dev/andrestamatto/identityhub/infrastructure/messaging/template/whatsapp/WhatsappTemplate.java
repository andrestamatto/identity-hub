package br.dev.andrestamatto.identityhub.infrastructure.messaging.template.whatsapp;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.WhatsappContent;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.WhatsappMediaContent;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.WhatsappMessageTemplate;

/**
 * Plain-text WhatsApp template renderer.
 * Implementations map notification details to short messages accepted by WhatsApp providers.
 */
public interface WhatsappTemplate {
    WhatsappMessageTemplate supports();
    WhatsappContent render(NotificationMessage notificationMessage);
}
