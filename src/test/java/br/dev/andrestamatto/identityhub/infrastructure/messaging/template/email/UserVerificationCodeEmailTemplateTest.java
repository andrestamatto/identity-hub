package br.dev.andrestamatto.identityhub.infrastructure.messaging.template.email;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.channels.NotificationChannels;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.EmailMessageTemplate;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.MessageTemplates;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.SmsMessageTemplate;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.template.email.UserVerificationCodeEmailTemplate;
import br.dev.andrestamatto.identityhub.support.UserTestData;
import org.junit.jupiter.api.Test;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserVerificationCodeEmailTemplateTest {

    @Test
    public void shouldRenderVerificationCodeEmailTemplateWithMessageDetails() {
        var template = new UserVerificationCodeEmailTemplate(templateEngine());
        var notificationMessage = NotificationMessage.create(
                UserTestData.validUsernameString,
                Map.of(
                        "verificationCode", UserTestData.validVerificationCode,
                        "expiresAt", "09/06/2026 10:15:00"
                ),
                new MessageTemplates(EmailMessageTemplate.EMAIL_USER_VERIFICATION_CODE, SmsMessageTemplate.UNDEFINED),
                NotificationChannels.email()
        );

        var body = template.render(notificationMessage);

        assertTrue(body.contains(UserTestData.validUsernameString));
        assertTrue(body.contains(UserTestData.validVerificationCode));
        assertTrue(body.contains("09/06/2026 10:15:00"));
    }

    private SpringTemplateEngine templateEngine() {
        var templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);

        var templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        return templateEngine;
    }
}
