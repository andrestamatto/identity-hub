package br.dev.andrestamatto.identityhub.infrastructure.messaging;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.EmailSender;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.templates.ConfirmationCodeEmailTemplate;

public class UserVerificationEmailSender implements EmailSender {

    private final ConfirmationCodeEmailTemplate confirmationCodeEmailTemplate;

    public UserVerificationEmailSender(ConfirmationCodeEmailTemplate confirmationCodeEmailTemplate) {
        this.confirmationCodeEmailTemplate = confirmationCodeEmailTemplate;
    }

    @Override
    public void send(String to, String subject, String body) {
        var message = confirmationCodeEmailTemplate.create(event.username().value(), event.verificationToken().code(), event.verificationToken().expiresAt());
        var subject = "Verify your identity";
    }
}
