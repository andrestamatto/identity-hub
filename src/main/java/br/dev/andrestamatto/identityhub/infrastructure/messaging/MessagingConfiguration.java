package br.dev.andrestamatto.identityhub.infrastructure.messaging;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.EmailSender;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.SmsSender;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.UserNotifier;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.templates.UserVerificationCodeEmailSender;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfiguration {

    @Bean
    public EmailSender emailSender(UserVerificationCodeEmailSender userVerificationCodeEmailSender) {
        return new UserVerificationEmailSender(userVerificationCodeEmailSender);
    }

    @Bean
    public SmsSender smsSender() {
        return new UserVerificationSmsSender();
    }

    @Bean
    public UserNotifier userNotifier(EmailSender emailSender, SmsSender smsSender) {
        return new UserVerificationNotifier(emailSender, smsSender);
    }


}
