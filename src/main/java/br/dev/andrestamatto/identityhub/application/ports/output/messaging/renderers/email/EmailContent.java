package br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.email;

/**
 * Rendered email ready for provider delivery.
 * At this point template selection has already happened and the body is final.
 */
public record EmailContent(
        String to,
        String subject,
        String body
) {

    public EmailContent {
        if (to == null || to.isBlank()) { throw new IllegalArgumentException("email recipient is required"); }
        if (subject == null || subject.isBlank()) { throw new IllegalArgumentException("email subject is required"); }
        if (body == null || body.isBlank()) { throw new IllegalArgumentException("email body is required"); }
    }
}
