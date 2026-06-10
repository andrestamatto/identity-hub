package br.dev.andrestamatto.identityhub.infrastructure.messaging.templates;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Spring configuration that registers email template renderers.
 * New email templates should be exposed here as EmailTemplate beans.
 */
@Configuration
public class TemplateConfiguration {

    @Bean
    public EmailTemplate userVerificationCodeEmailTemplate(SpringTemplateEngine engine) {
        return new UserVerificationCodeEmailTemplate(engine);
    }

    @Bean
    public EmailTemplate userWelcomeTemplate(SpringTemplateEngine engine) {
        return new UserWelcomeEmailTemplate(engine);
    }
}
