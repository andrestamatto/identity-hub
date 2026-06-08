package br.dev.andrestamatto.identityhub.application.ports.output.messaging;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.channels.NotificationChannels;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.MessageTemplates;

import java.util.Map;

/**
 * Message request produced by the application/infrastructure listener before delivery.
 * It describes the recipient, dynamic template data, selected templates, and delivery
 * channels, while leaving concrete providers (SMTP, Twilio, etc.) to infrastructure config.
 */
public record NotificationMessage(
    String recipient,
    Map<String, String> details,
    MessageTemplates messageTemplates,
    NotificationChannels notificationChannels
    ) {

    public NotificationMessage {
        if (recipient == null || recipient.isBlank()) {throw new IllegalArgumentException("recipient is null or blank");}
        if (messageTemplates == null) {throw new IllegalArgumentException("messageTemplates is null");}
        if (notificationChannels == null) {throw new IllegalArgumentException("notificationChannels is null");}
    }

    public static NotificationMessage create(String whoNotify, MessageTemplates messageTemplates, NotificationChannels notificationChannels) {
        return new NotificationMessage(whoNotify, null, messageTemplates, notificationChannels);
    }

    public static NotificationMessage create(String whoNotify, Map<String, String> details, MessageTemplates messageTemplates, NotificationChannels notificationChannels) {
        return new NotificationMessage(whoNotify, details, messageTemplates, notificationChannels);
    }
}
