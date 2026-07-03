package br.dev.andrestamatto.identityhub.infrastructure.messaging.template.whatsapp;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.channels.NotificationChannels;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.EmailMessageTemplate;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.MessageTemplates;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.SmsMessageTemplate;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.WhatsappMessageTemplate;
import br.dev.andrestamatto.identityhub.infrastructure.media.MediaProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TemplatedWhatsappRendererTest {

    @Test
    public void shouldRenderWhatsappMessageUsingSelectedTemplate() {
        var renderer = new TemplatedWhatsappRenderer(List.of(
                new UserVerificationCodeWhatsappTemplate(new MediaProperties("https://identityhub.dev/media"))
        ));

        var rendered = renderer.render(verificationNotification(WhatsappMessageTemplate.WHATSAPP_USER_VERIFICATION_CODE));

        assertEquals("+5511999998888", rendered.recipientNumber());
        assertEquals("https://identityhub.dev/media/whatsapp/identityhub.logo.png", rendered.mediaUrl());
    }

    @Test
    public void shouldFailWhenWhatsappTemplateIsNotRegistered() {
        var renderer = new TemplatedWhatsappRenderer(List.of());

        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> renderer.render(verificationNotification(WhatsappMessageTemplate.WHATSAPP_USER_VERIFICATION_CODE))
        );

        assertEquals("WhatsApp template not found: WHATSAPP_USER_VERIFICATION_CODE", exception.getMessage());
    }

    private NotificationMessage verificationNotification(WhatsappMessageTemplate whatsappTemplate) {
        return NotificationMessage.create(
                "+5511999998888",
                Map.of(
                        "verificationCode", "123456",
                        "expiresAt", "03/07/2026 10:30:00"
                ),
                new MessageTemplates(
                        EmailMessageTemplate.EMAIL_USER_VERIFICATION_CODE,
                        SmsMessageTemplate.SMS_USER_VERIFICATION_CODE,
                        whatsappTemplate
                ),
                NotificationChannels.whatsapp()
        );
    }
}
