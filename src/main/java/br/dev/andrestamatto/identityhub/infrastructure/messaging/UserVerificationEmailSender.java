package br.dev.andrestamatto.identityhub.infrastructure.messaging;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.EmailSender;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.MessageTemplate;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.templates.UserVerificationCodeEmailSender;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.templates.EmailTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class UserVerificationEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(UserVerificationEmailSender.class);

    private final UserVerificationCodeEmailSender userVerificationCodeEmailSender;

    public UserVerificationEmailSender(UserVerificationCodeEmailSender userVerificationCodeEmailSender) {
        this.userVerificationCodeEmailSender = userVerificationCodeEmailSender;
    }

    @Override
    public void send(NotificationMessage notificationMessage) {

        var messageBody = resolveMessageBody(notificationMessage);
        var subject = Optional.ofNullable(notificationMessage.details())
                .map( (nonNullDetails) ->
                        nonNullDetails.getOrDefault("subject", "IdentityHub notification") )
                .orElse("IdentityHub notification");

        // TODO integrate with a real provider (SES, SMTP, etc.)
        log.info("Sending email to {} with subject '{}'. Body size={}",
                notificationMessage.recipient(), subject, messageBody.length());
    }

    private EmailTemplate emailTemplate(MessageTemplate messageTemplate) {
        return switch(messageTemplate) {
            case USER_VERIFICATION_CODE -> userVerificationCodeEmailSender;
            default -> null;
        };
    }

    private String resolveMessageBody(NotificationMessage notificationMessage) {
        var emailTemplate = emailTemplate(notificationMessage.messageTemplate());
        if (emailTemplate != null) {
            return emailTemplate.create(
                    notificationMessage.recipient(),
                    notificationMessage.details()
            );
        }

        return Optional.ofNullable(notificationMessage.details())
                .map(details -> details.get("message"))
                .orElse("IdentityHub notification");
    }

}
