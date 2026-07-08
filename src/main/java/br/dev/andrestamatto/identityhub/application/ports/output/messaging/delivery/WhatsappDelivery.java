package br.dev.andrestamatto.identityhub.application.ports.output.messaging.delivery;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.WhatsappContent;

/**
 * Output port for delivering already rendered WhatsApp messages.
 */
public interface WhatsappDelivery {
    void deliver(WhatsappContent whatsappContent);
}
