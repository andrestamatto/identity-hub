package br.dev.andrestamatto.identityhub.infrastructure.messaging.template.whatsapp;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.WhatsappTextContent;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.WhatsappMessageTemplate;

public class UserWelcomeWhatsappTemplate implements WhatsappTemplate {

    @Override
    public WhatsappMessageTemplate supports() {
        return WhatsappMessageTemplate.WHATSAPP_USER_SUCCESSFULLY_ACTIVATED;
    }

    @Override
    public WhatsappTextContent render(NotificationMessage notificationMessage) {
        var formattedMessage = """
            Welcome to IdentityHub!
            Your account was successfully activated.
            """;

        return new WhatsappTextContent(
                notificationMessage.recipient(),
                formattedMessage
        );
    }
}
