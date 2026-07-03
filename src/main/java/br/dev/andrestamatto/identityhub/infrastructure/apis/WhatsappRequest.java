package br.dev.andrestamatto.identityhub.infrastructure.apis;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.RenderedWhatsapp;

public record WhatsappRequest(
        String number,
        String mediaType,
        String mediaUrl,
        String caption
) {
    public static WhatsappRequest from(RenderedWhatsapp renderedWhatsapp) {
        return new WhatsappRequest(
                renderedWhatsapp.recipientNumber(),
                renderedWhatsapp.mediaType().value(),
                renderedWhatsapp.mediaUrl(),
                renderedWhatsapp.caption()
        );
    }
}
