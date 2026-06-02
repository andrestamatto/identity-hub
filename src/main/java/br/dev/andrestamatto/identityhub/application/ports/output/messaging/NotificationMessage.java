package br.dev.andrestamatto.identityhub.application.ports.output.messaging;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.MessageTemplate;

import java.util.Map;

public record NotificationMessage(
    MessageTemplate messageTemplate,
    String recipient,
    Map<String, String> details
) {

    public NotificationMessage {
        if (recipient == null || recipient.isBlank()) {throw new IllegalArgumentException("recipient is null or blank");}
        if (messageTemplate == null) {throw new IllegalArgumentException("messageTemplate is null");}
    }

    public static NotificationMessage create(MessageTemplate messageTemplate, String whoNotify){
        return new NotificationMessage(messageTemplate, whoNotify, null);
    }

    public static NotificationMessage create(MessageTemplate messageTemplate, String whoNotify, Map<String, String> details) {
        return new NotificationMessage(messageTemplate, whoNotify, details);
    }
}
