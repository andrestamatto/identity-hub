package br.dev.andrestamatto.identityhub.infrastructure.messaging.sender.email;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.channels.NotificationChannels;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.delivery.EmailDelivery;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.email.RenderedEmail;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.EmailRenderer;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.EmailMessageTemplate;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.MessageTemplates;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.SmsMessageTemplate;
import br.dev.andrestamatto.identityhub.support.UserTestData;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultEmailSenderTest {

    @Test
    public void shouldRenderNotificationMessageBeforeDeliveringEmail() {
        var emailRenderer = mock(EmailRenderer.class);
        var emailDelivery = mock(EmailDelivery.class);
        var emailSender = new DefaultEmailSender(emailRenderer, emailDelivery);
        var notificationMessage = NotificationMessage.create(
                UserTestData.validUsernameString,
                new MessageTemplates(EmailMessageTemplate.EMAIL_USER_VERIFICATION_CODE, SmsMessageTemplate.UNDEFINED),
                NotificationChannels.email()
        );
        var renderedEmail = new RenderedEmail(UserTestData.validUsernameString, "Verify your identity", "<p>Code</p>");

        when(emailRenderer.render(notificationMessage)).thenReturn(renderedEmail);

        emailSender.send(notificationMessage);

        verify(emailRenderer).render(notificationMessage);
        verify(emailDelivery).deliver(renderedEmail);
    }
}
