package br.dev.andrestamatto.identityhub.infrastructure.messaging.delivery.whatsapp;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.delivery.WhatsappDelivery;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.RenderedWhatsapp;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.WhatsappMediaType;
import br.dev.andrestamatto.identityhub.infrastructure.apis.WhatsappApiClient;
import br.dev.andrestamatto.identityhub.infrastructure.apis.WhatsappRequest;

public class DefaultWhatsappDelivery implements WhatsappDelivery {

    private final WhatsappApiClient whatsappApiClient;

    public DefaultWhatsappDelivery(WhatsappApiClient whatsappApiClient) {
        this.whatsappApiClient = whatsappApiClient;
    }

    @Override
    public void deliver(RenderedWhatsapp whatsappContent) {
        var request = WhatsappRequest.from(whatsappContent);
        if (whatsappContent.mediaType() == WhatsappMediaType.TEXT) {
            whatsappApiClient.send(request);
        } else {
            whatsappApiClient.sendMedia(request);
        }
    }
}
