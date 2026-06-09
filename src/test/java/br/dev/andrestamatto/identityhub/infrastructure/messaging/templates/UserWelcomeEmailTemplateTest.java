package br.dev.andrestamatto.identityhub.infrastructure.messaging.templates;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.channels.NotificationChannels;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.EmailMessageTemplate;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.MessageTemplates;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.SmsMessageTemplate;
import br.dev.andrestamatto.identityhub.support.UserTestData;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserWelcomeEmailTemplateTest {

    @Test
    public void shouldRenderWelcomeEmailTemplateWithUsername() {
        var template = new UserWelcomeEmailTemplate(templateEngine());
        var notificationMessage = NotificationMessage.create(
                UserTestData.validUsernameString,
                Map.of("subject", "Welcome to IdentityHub"),
                new MessageTemplates(EmailMessageTemplate.EMAIL_USER_SUCCESSFULLY_ACTIVATED, SmsMessageTemplate.UNDEFINED),
                NotificationChannels.email()
        );

        var body = template.render(notificationMessage);

        assertTrue(body.contains("Account Verified"));
        assertTrue(body.contains(UserTestData.validUsernameString));
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
