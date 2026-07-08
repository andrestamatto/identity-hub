package br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp;

/**
 * Provider-ready WhatsApp content produced after template rendering.
 */
public record WhatsappTextContent(
        String recipientNumber,
        String message,
        WhatsappMediaType mediaType
) implements WhatsappContent {

    public WhatsappTextContent {
        if (recipientNumber == null || recipientNumber.isBlank()) {
            throw new IllegalArgumentException("WhatsApp recipient number is required.");
        }

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("WhatsApp message is required.");
        }
    }

    public WhatsappTextContent (String recipientNumber, String message) {
        this(recipientNumber, message, WhatsappMediaType.TEXT);
    }
}
