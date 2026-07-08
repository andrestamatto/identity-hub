package br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.WhatsappContent;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.WhatsappMediaContent;

public interface WhatsappRenderer {
    WhatsappContent render(NotificationMessage notificationMessage);
}
