package br.dev.andrestamatto.identityhub.infrastructure.messaging.delivery.email;

import br.dev.andrestamatto.identityhub.application.exceptions.EmailDeliveryException;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.delivery.EmailDelivery;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.email.RenderedEmail;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.config.NotificationProperties;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;

/**
 * SMTP implementation of EmailDelivery.
 * It is the provider-specific layer that will use NotificationProperties to send
 * already rendered email content through an SMTP server.
 */
public class SmtpEmailDelivery implements EmailDelivery {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailDelivery.class);

    private final NotificationProperties properties;
    private final JavaMailSender javaMailSender;

    public SmtpEmailDelivery(NotificationProperties properties, JavaMailSender javaMailSender) {
        this.properties = properties;
        this.javaMailSender = javaMailSender;
    }


    @Override
    @Retryable(
            retryFor = MailException.class,
            maxAttemptsExpression = "${identity-hub.notification.email.smtp.max-attempts:3}",
            backoff = @Backoff(delayExpression = "${identity-hub.notification.email.smtp.retry-backoff-millis:300}")
    )
    public void deliver(RenderedEmail email) {
        try {
            log.info(
                    "SMTP email delivery started. host={} port={} subject={}",
                    properties.email().smtp().host(),
                    properties.email().smtp().port(),
                    email.subject()
            );
            var message = javaMailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(properties.email().from());
            helper.setTo(email.to());
            helper.setSubject(email.subject());
            helper.setText(email.body(), true);

            javaMailSender.send(message);
            log.info(
                    "SMTP email delivery completed. host={} port={} subject={}",
                    properties.email().smtp().host(),
                    properties.email().smtp().port(),
                    email.subject()
            );
        } catch (MessagingException exception) {
            log.error("SMTP email message preparation failed. subject={}", email.subject(), exception);
            throw new EmailDeliveryException("Email message could not be prepared for SMTP delivery.", exception);
        }
    }

    @Recover
    public void recover(MailException exception, RenderedEmail email) {
        throw new EmailDeliveryException(errorMessageFrom(exception), exception);
    }

    private String errorMessageFrom(MailException exception) {
        var message = String.valueOf(exception.getMessage()).toLowerCase();

        if (message.contains("timeout") || message.contains("timed out")) {
            return "Email delivery timed out while connecting to the SMTP provider.";
        }

        if (message.contains("connection refused") || message.contains("couldn't connect")) {
            return "Email delivery provider is unavailable.";
        }

        return "Email delivery failed through the SMTP provider.";
    }
}
