package br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp;

/**
 * Provider-ready WhatsApp content produced after template rendering.
 */
public record RenderedWhatsapp(
        String recipientNumber,
        WhatsappMediaType mediaType,
        String mediaUrl,
        String caption
) {
    public RenderedWhatsapp {
        if (recipientNumber == null || recipientNumber.isBlank()) {
            throw new IllegalArgumentException("WhatsApp recipient number is required.");
        }

        if (mediaType == null) {
            throw new IllegalArgumentException("WhatsApp media type is required.");
        }

        if (mediaType != WhatsappMediaType.TEXT && (mediaUrl == null || mediaUrl.isBlank())) {
            throw new IllegalArgumentException("WhatsApp media URL is required for media messages.");
        }

        if (caption == null || caption.isBlank()) {
            throw new IllegalArgumentException("WhatsApp caption is required.");
        }
    }
}
