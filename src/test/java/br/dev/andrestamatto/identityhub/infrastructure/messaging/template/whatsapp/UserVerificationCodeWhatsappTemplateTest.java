package br.dev.andrestamatto.identityhub.infrastructure.messaging.template.whatsapp;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.channels.NotificationChannels;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.WhatsappMediaType;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.EmailMessageTemplate;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.MessageTemplates;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.SmsMessageTemplate;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.WhatsappMessageTemplate;
import br.dev.andrestamatto.identityhub.infrastructure.media.MediaProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserVerificationCodeWhatsappTemplateTest {

    @Test
    public void shouldRenderVerificationCodeWhatsappMessageWithImageMedia() {
        var template = new UserVerificationCodeWhatsappTemplate(
                new MediaProperties("https://identityhub.dev/media")
        );
        var notificationMessage = NotificationMessage.create(
                "+5511999998888",
                Map.of(
                        "verificationCode", "123456",
                        "expiresAt", "03/07/2026 10:30:00"
                ),
                new MessageTemplates(
                        EmailMessageTemplate.EMAIL_USER_VERIFICATION_CODE,
                        SmsMessageTemplate.SMS_USER_VERIFICATION_CODE,
                        WhatsappMessageTemplate.WHATSAPP_USER_VERIFICATION_CODE
                ),
                NotificationChannels.whatsapp()
        );

        var rendered = template.render(notificationMessage);

        assertEquals("+5511999998888", rendered.recipientNumber());
        assertEquals(WhatsappMediaType.IMAGE, rendered.mediaType());
        assertEquals("https://identityhub.dev/media/whatsapp/identityhub.logo.png", rendered.mediaUrl());
        assertTrue(rendered.caption().contains("123456"));
        assertTrue(rendered.caption().contains("03/07/2026 10:30:00"));
    }
}
