package br.dev.andrestamatto.identityhub.infrastructure.messaging.templates;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.channels.NotificationChannels;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.EmailMessageTemplate;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.MessageTemplates;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.SmsMessageTemplate;
import br.dev.andrestamatto.identityhub.support.UserTestData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TemplatedEmailRendererTest {

    @Test
    public void shouldRenderEmailUsingTemplateSelectedByNotificationMessage() {
        var renderer = new TemplatedEmailRenderer(List.of(new FixedEmailTemplate()));
        var notificationMessage = NotificationMessage.create(
                UserTestData.validUsernameString,
                Map.of("subject", "Verify your identity"),
                new MessageTemplates(EmailMessageTemplate.EMAIL_USER_VERIFICATION_CODE, SmsMessageTemplate.UNDEFINED),
                NotificationChannels.email()
        );

        var renderedEmail = renderer.render(notificationMessage);

        assertEquals(UserTestData.validUsernameString, renderedEmail.to());
        assertEquals("Verify your identity", renderedEmail.subject());
        assertEquals("Rendered verification code email", renderedEmail.body());
    }

    @Test
    public void shouldUseDefaultSubjectWhenNotificationMessageDoesNotProvideOne() {
        var renderer = new TemplatedEmailRenderer(List.of(new FixedEmailTemplate()));
        var notificationMessage = NotificationMessage.create(
                UserTestData.validUsernameString,
                Map.of(),
                new MessageTemplates(EmailMessageTemplate.EMAIL_USER_VERIFICATION_CODE, SmsMessageTemplate.UNDEFINED),
                NotificationChannels.email()
        );

        var renderedEmail = renderer.render(notificationMessage);

        assertEquals("IdentityHub notification", renderedEmail.subject());
    }

    @Test
    public void shouldRejectUnknownEmailTemplate() {
        var renderer = new TemplatedEmailRenderer(List.of());
        var notificationMessage = NotificationMessage.create(
                UserTestData.validUsernameString,
                new MessageTemplates(EmailMessageTemplate.EMAIL_USER_VERIFICATION_CODE, SmsMessageTemplate.UNDEFINED),
                NotificationChannels.email()
        );

        assertThrows(IllegalArgumentException.class, () -> renderer.render(notificationMessage));
    }

    private static class FixedEmailTemplate implements EmailTemplate {

        @Override
        public EmailMessageTemplate supports() {
            return EmailMessageTemplate.EMAIL_USER_VERIFICATION_CODE;
        }

        @Override
        public String render(NotificationMessage notificationMessage) {
            return "Rendered verification code email";
        }
    }
}
