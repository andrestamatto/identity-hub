package br.dev.andrestamatto.identityhub.infrastructure.messaging.config;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.delivery.EmailDelivery;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.delivery.SmsDelivery;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.notifiers.UserNotifier;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.EmailRenderer;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.SmsRenderer;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.senders.EmailSender;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.senders.SmsSender;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.UserVerificationNotifier;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.delivery.email.SmtpEmailDelivery;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.delivery.sms.LoggingSmsDelivery;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.delivery.sms.TwilioSmsDelivery;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.sender.email.DefaultEmailSender;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.sender.sms.DefaultSmsSender;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.template.email.EmailTemplate;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.template.email.TemplatedEmailRenderer;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.template.sms.SmsTemplate;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.template.sms.TemplatedSmsRenderer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.retry.annotation.EnableRetry;

import java.util.List;

@Configuration
@EnableRetry
@EnableConfigurationProperties(NotificationProperties.class)
public class MessagingConfiguration {

    @Bean
    public EmailRenderer emailRenderer(List<EmailTemplate> templates) {
        return new TemplatedEmailRenderer(templates);
    }

    @Bean
    public SmsRenderer smsRenderer(List<SmsTemplate> templates) {
        return new TemplatedSmsRenderer(templates);
    }

    @Bean
    public EmailSender emailSender(EmailRenderer emailRenderer, EmailDelivery emailDelivery) {
        return new DefaultEmailSender(emailRenderer, emailDelivery);
    }

    @Bean
    public SmsSender smsSender(SmsRenderer smsRenderer, SmsDelivery smsDelivery) {
        return new DefaultSmsSender(smsRenderer, smsDelivery);
    }

    @Bean
    public UserNotifier userVerificationNotifier(EmailSender emailSender, SmsSender smsSender) {
        return new UserVerificationNotifier(emailSender, smsSender);
    }

    @Bean
    @ConditionalOnProperty(prefix = "identity-hub.notification.email", name = "provider", havingValue = "smtp")
    public EmailDelivery smtpEmailDelivery(NotificationProperties properties, JavaMailSender javaMailSender) {
        return new SmtpEmailDelivery(properties, javaMailSender);
    }

    @Bean
    @ConditionalOnMissingBean(SmsDelivery.class)
    @ConditionalOnProperty(prefix = "identity-hub.notification.sms", name = "provider", havingValue = "log", matchIfMissing = true)
    public SmsDelivery loggingSmsDelivery() {
        return new LoggingSmsDelivery();
    }

    @Bean
    @ConditionalOnMissingBean(SmsDelivery.class)
    @ConditionalOnProperty(prefix = "identity-hub.notification.sms", name = "provider", havingValue = "twilio")
    public SmsDelivery twilioSmsDelivery(NotificationProperties properties) {
        return new TwilioSmsDelivery(properties.sms().providers());
    }

    @Bean
    @ConditionalOnProperty(prefix = "identity-hub.notification.email", name = "provider", havingValue = "smtp")
    public JavaMailSender javaMailSender(NotificationProperties properties) {
        var email = properties.email();
        var smtp = email.smtp();

        var sender = new JavaMailSenderImpl();
        sender.setHost(smtp.host());
        sender.setPort(smtp.port());

        if (smtp.username() != null && !smtp.username().isBlank()) {
            sender.setUsername(smtp.username());
        }

        if (smtp.password() != null && !smtp.password().isBlank()) {
            sender.setPassword(smtp.password());
        }

        var javaMailProperties = sender.getJavaMailProperties();
        javaMailProperties.put("mail.transport.protocol", "smtp");
        javaMailProperties.put("mail.smtp.auth", String.valueOf(smtp.auth()));
        javaMailProperties.put("mail.smtp.starttls.enable", String.valueOf(smtp.starttls()));
        javaMailProperties.put("mail.smtp.connectiontimeout", String.valueOf(timeoutOrDefault(smtp.connectionTimeout())));
        javaMailProperties.put("mail.smtp.timeout", String.valueOf(timeoutOrDefault(smtp.readTimeout())));
        javaMailProperties.put("mail.smtp.writetimeout", String.valueOf(timeoutOrDefault(smtp.writeTimeout())));

        return sender;
    }

    private int timeoutOrDefault(int configuredTimeout) {
        return configuredTimeout > 0 ? configuredTimeout : 3000;
    }

}
