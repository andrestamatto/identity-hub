package br.dev.andrestamatto.identityhub.infrastructure.messaging.sender.whatsapp;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.delivery.WhatsappDelivery;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.WhatsappRenderer;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.senders.WhatsappSender;

/**
 * Default WhatsApp sender implementation that coordinates rendering and delivery.
 * It keeps message rendering separate from provider-specific WhatsApp transport.
 */
public class DefaultWhatsappSender implements WhatsappSender {

    private final WhatsappRenderer whatsappRenderer;
    private final WhatsappDelivery whatsappDelivery;

    public DefaultWhatsappSender(WhatsappRenderer whatsappRenderer, WhatsappDelivery whatsappDelivery) {
        this.whatsappRenderer = whatsappRenderer;
        this.whatsappDelivery = whatsappDelivery;
    }

    @Override
    public void send(NotificationMessage notificationMessage) {
        var renderedWhatsapp = whatsappRenderer.render(notificationMessage);
        whatsappDelivery.deliver(renderedWhatsapp);
    }
}
