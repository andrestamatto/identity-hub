package br.dev.andrestamatto.identityhub.infrastructure.messaging.delivery;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.delivery.EmailDelivery;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.email.RenderedEmail;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.config.NotificationProperties;

/**
 * SMTP implementation of EmailDelivery.
 * It is the provider-specific layer that will use NotificationProperties to send
 * already rendered email content through an SMTP server.
 */
public class SmtpEmailDelivery implements EmailDelivery {

    private final NotificationProperties properties;

    public SmtpEmailDelivery(NotificationProperties properties) {
        this.properties = properties;
    }


    @Override
    public void deliver(RenderedEmail email) {

    }
}
