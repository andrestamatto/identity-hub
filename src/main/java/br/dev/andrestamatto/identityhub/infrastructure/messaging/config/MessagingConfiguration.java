package br.dev.andrestamatto.identityhub.infrastructure.messaging.config;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.delivery.EmailDelivery;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.EmailRenderer;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.senders.EmailSender;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.senders.SmsSender;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.notifiers.UserNotifier;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.UserVerificationNotifier;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.UserVerificationSmsSender;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.email.DefaultEmailSender;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.smtp.SmtpEmailDelivery;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.templates.EmailTemplate;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.templates.TemplatedEmailRenderer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableConfigurationProperties(NotificationProperties.class)
public class MessagingConfiguration {

    @Bean
    public EmailRenderer emailRenderer(List<EmailTemplate> templates) {
        return new TemplatedEmailRenderer(templates);
    }

    @Bean
    public EmailSender emailSender(EmailRenderer emailRenderer, EmailDelivery emailDelivery) {
        return new DefaultEmailSender(emailRenderer, emailDelivery);
    }

    @Bean
    @ConditionalOnProperty(prefix = "identity-hub.notification.email", name = "provider", havingValue = "smtp")
    public EmailDelivery smtpEmailDelivery(NotificationProperties properties) {
        return new SmtpEmailDelivery(properties);
    }

    @Bean
    public SmsSender userVerificationSmsSender() {
        return new UserVerificationSmsSender();
    }

    @Bean
    public UserNotifier userVerificationNotifier(EmailSender emailSender, SmsSender smsSender) {
        return new UserVerificationNotifier(emailSender, smsSender);
    }


}
