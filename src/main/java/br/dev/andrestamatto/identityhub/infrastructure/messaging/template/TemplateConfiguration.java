package br.dev.andrestamatto.identityhub.infrastructure.messaging.template;

import br.dev.andrestamatto.identityhub.infrastructure.messaging.template.email.EmailTemplate;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.template.email.UserVerificationCodeEmailTemplate;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.template.email.UserWelcomeEmailTemplate;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.template.sms.SmsTemplate;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.template.sms.UserVerificationCodeSmsTemplate;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.template.sms.UserWelcomeSmsTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Spring configuration that registers notification template renderers.
 * New channel templates should be exposed here as EmailTemplate or SmsTemplate beans.
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

    @Bean
    public SmsTemplate userVerificationCodeSmsTemplate() {
        return new UserVerificationCodeSmsTemplate();
    }

    @Bean
    public SmsTemplate userWelcomeSmsTemplate() {
        return new UserWelcomeSmsTemplate();
    }
}
