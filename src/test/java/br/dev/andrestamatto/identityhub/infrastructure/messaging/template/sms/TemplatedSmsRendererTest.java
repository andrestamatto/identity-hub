package br.dev.andrestamatto.identityhub.infrastructure.messaging.template.sms;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.channels.NotificationChannels;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.EmailMessageTemplate;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.MessageTemplates;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.SmsMessageTemplate;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.template.sms.TemplatedSmsRenderer;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.template.sms.UserVerificationCodeSmsTemplate;
import br.dev.andrestamatto.identityhub.support.UserTestData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TemplatedSmsRendererTest {

    private static final String VALID_PHONE_NUMBER = "+5511999998888";

    @Test
    public void shouldRenderSmsUsingTemplateSelectedByNotificationMessage() {
        var renderer = new TemplatedSmsRenderer(List.of(new UserVerificationCodeSmsTemplate()));
        var notificationMessage = NotificationMessage.create(
                VALID_PHONE_NUMBER,
                Map.of(
                        "verificationCode", UserTestData.validVerificationCode,
                        "expiresAt", "10/06/2026 15:00:00"
                ),
                new MessageTemplates(
                        EmailMessageTemplate.EMAIL_USER_VERIFICATION_CODE,
                        SmsMessageTemplate.SMS_USER_VERIFICATION_CODE
                ),
                NotificationChannels.sms()
        );

        var renderedSms = renderer.render(notificationMessage);

        assertEquals(VALID_PHONE_NUMBER, renderedSms.to());
        assertEquals("IdentityHub code: 123456. Expires at 10/06/2026 15:00:00.", renderedSms.body());
    }

    @Test
    public void shouldFailWhenSmsTemplateIsNotRegistered() {
        var renderer = new TemplatedSmsRenderer(List.of());
        var notificationMessage = NotificationMessage.create(
                VALID_PHONE_NUMBER,
                Map.of("verificationCode", UserTestData.validVerificationCode),
                new MessageTemplates(
                        EmailMessageTemplate.EMAIL_USER_VERIFICATION_CODE,
                        SmsMessageTemplate.SMS_USER_VERIFICATION_CODE
                ),
                NotificationChannels.sms()
        );

        assertThrows(IllegalArgumentException.class, () -> renderer.render(notificationMessage));
    }
}
