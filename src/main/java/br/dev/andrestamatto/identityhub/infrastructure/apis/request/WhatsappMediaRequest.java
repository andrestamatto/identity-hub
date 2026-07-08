package br.dev.andrestamatto.identityhub.infrastructure.apis.request;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.WhatsappMediaContent;

public record WhatsappMediaRequest(
        String number,
        String caption,
        String mediaUrl,
        String mediaType
) {
    public static WhatsappMediaRequest from(WhatsappMediaContent whatsappMediaContent) {
        return new WhatsappMediaRequest(
                whatsappMediaContent.recipientNumber(),
                whatsappMediaContent.caption(),
                whatsappMediaContent.mediaUrl(),
                whatsappMediaContent.mediaType().value()
        );
    }
}
