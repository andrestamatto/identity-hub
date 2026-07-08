package br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.sms;

/**
 * Provider-ready SMS content produced after template rendering.
 * It contains only the destination number and final plain-text body.
 */
public record SmsContent(
        String to,
        String body
) {
    public SmsContent {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("SMS destination is required.");
        }

        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("SMS body is required.");
        }
    }
}
