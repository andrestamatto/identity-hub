package br.dev.andrestamatto.identityhub.infrastructure.messaging;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.channels.NotificationChannel;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.notifiers.UserNotifier;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.senders.EmailSender;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.senders.SmsSender;
import br.dev.andrestamatto.identityhub.domain.valueobjects.NotificationMethod;

/**
 * User notification router used by IdentityHub events.
 * It delegates the same NotificationMessage to the configured channel senders
 * according to the channels requested by the message.
 */
public class UserVerificationNotifier implements UserNotifier {

    private final EmailSender emailSender;
    private final SmsSender smsSender;

    public UserVerificationNotifier(EmailSender emailSender, SmsSender smsSender) {
        this.emailSender = emailSender;
        this.smsSender = smsSender;
    }

    @Override
    public void notify(NotificationMessage notificationMessage, NotificationMethod method) {
        for (var channel : notificationMessage.notificationChannels().values()) {
            send(notificationMessage, channel);
        }
    }

    private void send(NotificationMessage notificationMessage, NotificationChannel channel) {
        switch (channel) {
            case EMAIL -> emailSender.send(notificationMessage);
            case SMS -> smsSender.send(notificationMessage);
            case WHATSAPP -> throw new UnsupportedOperationException("WhatsApp notification is not implemented yet.");
        }
    }
}
