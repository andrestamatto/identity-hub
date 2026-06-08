package br.dev.andrestamatto.identityhub.infrastructure.messaging.smtp;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.EmailDelivery;

public class SmtpEmailDelivery implements EmailDelivery {

    @Override
    public void deliver(String to, String subject, String body) {

    }
}
