package br.dev.andrestamatto.identityhub.infrastructure.messaging.delivery;

import br.dev.andrestamatto.identityhub.application.exceptions.EmailDeliveryException;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.email.RenderedEmail;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.config.NotificationProperties;
import br.dev.andrestamatto.identityhub.support.UserTestData;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SmtpEmailDeliveryTest {

    @Test
    public void shouldSendRenderedEmailThroughJavaMailSender() throws Exception {
        var javaMailSender = mock(JavaMailSender.class);
        var mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        var delivery = new SmtpEmailDelivery(notificationProperties(), javaMailSender);
        var renderedEmail = new RenderedEmail(
                UserTestData.validUsernameString,
                "Verify your identity",
                "<p>Code</p>"
        );

        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        delivery.deliver(renderedEmail);

        var messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(messageCaptor.capture());
        assertEquals("Verify your identity", messageCaptor.getValue().getSubject());
        assertEquals(UserTestData.validUsernameString, messageCaptor.getValue().getRecipients(Message.RecipientType.TO)[0].toString());
    }

    @Test
    public void shouldRecoverSmtpTimeoutAsEmailDeliveryException() {
        var javaMailSender = mock(JavaMailSender.class);
        var delivery = new SmtpEmailDelivery(notificationProperties(1, 0), javaMailSender);
        var renderedEmail = new RenderedEmail(
                UserTestData.validUsernameString,
                "Verify your identity",
                "<p>Code</p>"
        );

        var exception = assertThrows(
                EmailDeliveryException.class,
                () -> delivery.recover(new MailSendException("timeout"), renderedEmail)
        );

        assertEquals("Email delivery timed out while connecting to the SMTP provider.", exception.getMessage());
    }

    private NotificationProperties notificationProperties() {
        return notificationProperties(3, 300);
    }

    private NotificationProperties notificationProperties(int maxAttempts, int retryBackoffMillis) {
        return new NotificationProperties(
                new NotificationProperties.EmailNotification(
                        true,
                        "smtp",
                        "no-reply@identityhub.dev",
                        new NotificationProperties.Smtp(
                                "localhost",
                                1025,
                                null,
                                null,
                                false,
                                false,
                                3000,
                                3000,
                                3000,
                                maxAttempts,
                                retryBackoffMillis
                        )
                )
        );
    }
}
