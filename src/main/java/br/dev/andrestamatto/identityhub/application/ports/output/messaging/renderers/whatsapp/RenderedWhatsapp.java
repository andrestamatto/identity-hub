package br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp;

/**
 * Provider-ready WhatsApp content produced after template rendering.
 */
public record RenderedWhatsapp(
        String recipientNumber,
        String body
) {
    public RenderedWhatsapp {
        if (recipientNumber == null || recipientNumber.isBlank()) {
            throw new IllegalArgumentException("Whatsapp recipient number is required.");
        }

        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Whatsapp body is required.");
        }
    }
}
