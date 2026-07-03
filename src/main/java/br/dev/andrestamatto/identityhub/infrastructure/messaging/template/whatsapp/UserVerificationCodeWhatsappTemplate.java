package br.dev.andrestamatto.identityhub.infrastructure.messaging.template.whatsapp;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.RenderedWhatsapp;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.WhatsappMediaType;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.WhatsappMessageTemplate;
import br.dev.andrestamatto.identityhub.infrastructure.media.MediaProperties;

public class UserVerificationCodeWhatsappTemplate implements WhatsappTemplate {

    private final MediaProperties mediaProperties;

    public UserVerificationCodeWhatsappTemplate(MediaProperties mediaProperties) {
        this.mediaProperties = mediaProperties;
    }

    @Override
    public WhatsappMessageTemplate supports() {
        return WhatsappMessageTemplate.WHATSAPP_USER_VERIFICATION_CODE;
    }

    @Override
    public RenderedWhatsapp render(NotificationMessage notificationMessage) {
        var details = notificationMessage.details();

        var formattedCaption = """
                Hello, thank you for joining us!
                Your _*IdentityHub verification code*_ is:
                
                ━━━━━━
                %s
                ━━━━━━
                
                This code expires in:
                _%s_
                """
                .formatted(
                        details.get("verificationCode"),
                        details.get("expiresAt")
                );

        return new RenderedWhatsapp(
                notificationMessage.recipient(),
                WhatsappMediaType.IMAGE,
                mediaProperties.baseUrl() + "/whatsapp/identityhub.logo.png",
                formattedCaption
        );
    }
}
