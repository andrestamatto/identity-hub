package br.dev.andrestamatto.identityhub.infrastructure.messaging.sender.whatsapp;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.channels.NotificationChannels;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.delivery.WhatsappDelivery;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.WhatsappRenderer;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.WhatsappMediaContent;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.WhatsappMediaType;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.EmailMessageTemplate;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.MessageTemplates;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.SmsMessageTemplate;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.WhatsappMessageTemplate;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultWhatsappSenderTest {

    @Test
    public void shouldRenderAndDeliverWhatsappNotification() {
        var renderer = mock(WhatsappRenderer.class);
        var delivery = mock(WhatsappDelivery.class);
        var sender = new DefaultWhatsappSender(renderer, delivery);
        var notificationMessage = NotificationMessage.create(
                "+5511999998888",
                new MessageTemplates(
                        EmailMessageTemplate.EMAIL_USER_VERIFICATION_CODE,
                        SmsMessageTemplate.SMS_USER_VERIFICATION_CODE,
                        WhatsappMessageTemplate.WHATSAPP_USER_VERIFICATION_CODE
                ),
                NotificationChannels.whatsapp()
        );
        var rendered = new WhatsappMediaContent(
                "+5511999998888",
                "IdentityHub code: 123456",
                "https://identityhub.dev/media/whatsapp/identityhub.logo.png",
                WhatsappMediaType.IMAGE
        );
        when(renderer.render(notificationMessage)).thenReturn(rendered);

        sender.send(notificationMessage);

        verify(renderer).render(notificationMessage);
        verify(delivery).deliver(rendered);
    }
}
