package br.dev.andrestamatto.identityhub.infrastructure.messaging;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.SmsSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserVerificationSmsSender implements SmsSender {
    private static final Logger log = LoggerFactory.getLogger(UserVerificationSmsSender.class);

    @Override
    public void send(NotificationMessage notificationMessage) {
        var message = notificationMessage.details() != null
                ? notificationMessage.details().getOrDefault("message", "IdentityHub verification")
                : "IdentityHub verification";

        // TODO integrate with a real provider (Twilio, SNS, etc.)
        log.info("Sending SMS to {}. Message size={}", notificationMessage.recipient(), message.length());
    }
}
