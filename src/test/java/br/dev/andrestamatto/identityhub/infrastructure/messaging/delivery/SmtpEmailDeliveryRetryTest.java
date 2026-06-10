package br.dev.andrestamatto.identityhub.infrastructure.messaging.delivery;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.delivery.EmailDelivery;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.email.RenderedEmail;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.config.NotificationProperties;
import br.dev.andrestamatto.identityhub.support.UserTestData;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(SmtpEmailDeliveryRetryTest.Config.class)
@TestPropertySource(properties = {
        "identity-hub.notification.email.smtp.max-attempts=2",
        "identity-hub.notification.email.smtp.retry-backoff-millis=0"
})
public class SmtpEmailDeliveryRetryTest {

    @Autowired
    private EmailDelivery delivery;

    @Autowired
    private JavaMailSender javaMailSender;

    @BeforeEach
    public void setup() {
        reset(javaMailSender);
    }

    @Test
    public void shouldRetrySmtpDeliveryWhenFirstAttemptFails() {
        var firstMessage = new MimeMessage(Session.getInstance(new Properties()));
        var secondMessage = new MimeMessage(Session.getInstance(new Properties()));
        var renderedEmail = new RenderedEmail(
                UserTestData.validUsernameString,
                "Verify your identity",
                "<p>Code</p>"
        );

        when(javaMailSender.createMimeMessage()).thenReturn(firstMessage, secondMessage);
        doThrow(new MailSendException("timeout"))
                .doNothing()
                .when(javaMailSender)
                .send(any(MimeMessage.class));

        delivery.deliver(renderedEmail);

        verify(javaMailSender, times(2)).send(any(MimeMessage.class));
    }

    @Configuration
    @EnableRetry
    static class Config {

        @Bean
        JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }

        @Bean
        NotificationProperties notificationProperties() {
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
                                    2,
                                    0
                            )
                    )
            );
        }

        @Bean
        EmailDelivery emailDelivery(NotificationProperties properties, JavaMailSender javaMailSender) {
            return new SmtpEmailDelivery(properties, javaMailSender);
        }
    }
}
