package br.dev.andrestamatto.identityhub.infrastructure.messaging.template;

import br.dev.andrestamatto.identityhub.infrastructure.media.MediaProperties;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.template.email.EmailTemplate;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.template.email.UserVerificationCodeEmailTemplate;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.template.email.UserWelcomeEmailTemplate;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.template.sms.SmsTemplate;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.template.sms.UserVerificationCodeSmsTemplate;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.template.sms.UserWelcomeSmsTemplate;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.template.whatsapp.UserVerificationCodeWhatsappTemplate;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.template.whatsapp.WhatsappTemplate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Spring configuration that registers notification template renderers.
 * New channel templates should be exposed here as channel-specific template beans.
 */
@Configuration
@EnableConfigurationProperties(MediaProperties.class)
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


    @Bean
    public WhatsappTemplate userVerificationCodeWhatsappTemplate(MediaProperties mediaProperties) {
        return new UserVerificationCodeWhatsappTemplate(mediaProperties);
    }
}
