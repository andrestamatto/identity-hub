package br.dev.andrestamatto.identityhub.infrastructure.messaging.templates;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Configuration
public class TemplateConfiguration {

    @Bean
    public UserVerificationCodeEmailSender userVerificationCodeEmailSender(SpringTemplateEngine springTemplateEngine) {
        return new UserVerificationCodeEmailSender(springTemplateEngine);
    }
}
