package br.dev.andrestamatto.identityhub.infrastructure.messaging;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.EmailSender;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.SmsSender;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.UserNotifier;
import br.dev.andrestamatto.identityhub.domain.valueobjects.NotificationMethod;

public class UserVerificationNotifier implements UserNotifier {

    private final EmailSender emailSender;
    private final SmsSender smsSender;

    public UserVerificationNotifier(EmailSender emailSender, SmsSender smsSender) {
        this.emailSender = emailSender;
        this.smsSender = smsSender;
    }


    @Override
    public void notify(NotificationMessage notificationMessage, NotificationMethod method) {
        validate(notificationMessage, method);

        switch (method) {
            case EMAIL -> emailSender.send(notificationMessage);
            case SMS -> smsSender.send(notificationMessage);
            case BOTH -> {
                emailSender.send(notificationMessage);
                smsSender.send(notificationMessage);
            }
        }
    }

    private void validate(NotificationMessage notificationMessage, NotificationMethod method) {
        if (notificationMessage == null) {throw new IllegalArgumentException("notificationMessage is null");}
        if (method == null) {throw new IllegalArgumentException("methodToNotify is null");}

    }
}
