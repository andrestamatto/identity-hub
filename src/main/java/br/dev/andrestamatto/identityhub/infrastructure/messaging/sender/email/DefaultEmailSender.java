package br.dev.andrestamatto.identityhub.infrastructure.messaging.sender.email;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.delivery.EmailDelivery;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.EmailRenderer;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.senders.EmailSender;

/**
 * Default EmailSender implementation that coordinates rendering and delivery.
 * It keeps template rendering separate from provider-specific email transport.
 */
public class DefaultEmailSender implements EmailSender {

    private final EmailRenderer emailRenderer;
    private final EmailDelivery emailDelivery;

    public DefaultEmailSender(EmailRenderer emailRenderer, EmailDelivery emailDelivery) {
        this.emailRenderer = emailRenderer;
        this.emailDelivery = emailDelivery;
    }

    @Override
    public void send(NotificationMessage notificationMessage) {
        var renderedEmail = emailRenderer.render(notificationMessage);
        emailDelivery.deliver(renderedEmail);
    }
}
