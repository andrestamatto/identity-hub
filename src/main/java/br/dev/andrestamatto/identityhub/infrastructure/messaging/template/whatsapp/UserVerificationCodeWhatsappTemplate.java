package br.dev.andrestamatto.identityhub.infrastructure.messaging.template.whatsapp;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.WhatsappMessageTemplate;

public class UserVerificationCodeWhatsappTemplate implements WhatsappTemplate {

    @Override
    public WhatsappMessageTemplate supports() {
        return WhatsappMessageTemplate.WHATSAPP_USER_VERIFICATION_CODE;
    }

    @Override
    public String render(NotificationMessage notificationMessage) {
        var details = notificationMessage.details();

        return """
                {
                    "number":"%s",
                    "mediaType": "image",
                    "mediaUrl": "",
                    "caption":"%s"
                }
                """
                .formatted(
                        notificationMessage.recipient(),
                        "IdentityHub code: " + details.get("verificationCode") + ". Expires at " + details.get("expiresAt") + "."
                );
    }
}
