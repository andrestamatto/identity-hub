package br.dev.andrestamatto.identityhub.infrastructure.messaging.sender.sms;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.channels.NotificationChannels;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.delivery.SmsDelivery;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.SmsRenderer;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.sms.RenderedSms;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.EmailMessageTemplate;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.MessageTemplates;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.SmsMessageTemplate;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.sender.sms.DefaultSmsSender;
import br.dev.andrestamatto.identityhub.support.UserTestData;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultSmsSenderTest {

    private static final String VALID_PHONE_NUMBER = "+5511999998888";

    @Test
    public void shouldRenderNotificationMessageBeforeDeliveringSms() {
        var smsRenderer = mock(SmsRenderer.class);
        var smsDelivery = mock(SmsDelivery.class);
        var smsSender = new DefaultSmsSender(smsRenderer, smsDelivery);
        var notificationMessage = NotificationMessage.create(
                VALID_PHONE_NUMBER,
                Map.of("verificationCode", UserTestData.validVerificationCode),
                new MessageTemplates(
                        EmailMessageTemplate.EMAIL_USER_VERIFICATION_CODE,
                        SmsMessageTemplate.SMS_USER_VERIFICATION_CODE
                ),
                NotificationChannels.sms()
        );
        var renderedSms = new RenderedSms(VALID_PHONE_NUMBER, "IdentityHub code: 123456.");

        when(smsRenderer.render(notificationMessage)).thenReturn(renderedSms);

        smsSender.send(notificationMessage);

        verify(smsRenderer).render(notificationMessage);
        verify(smsDelivery).deliver(renderedSms);
    }
}
