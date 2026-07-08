package br.dev.andrestamatto.identityhub.infrastructure.apis.request;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.WhatsappTextContent;

public record WhatsappTextRequest(
        String number,
        String message
) {
    public static WhatsappTextRequest from(WhatsappTextContent whatsappTextContent) {
        return new WhatsappTextRequest(
                whatsappTextContent.recipientNumber(),
                whatsappTextContent.message()
        );
    }
}
