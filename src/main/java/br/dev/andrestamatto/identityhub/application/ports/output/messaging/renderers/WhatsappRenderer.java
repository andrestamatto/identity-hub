package br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.RenderedWhatsapp;

public interface WhatsappRenderer {
    RenderedWhatsapp render(NotificationMessage notificationMessage);
}
