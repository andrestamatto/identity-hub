package br.dev.andrestamatto.identityhub.infrastructure.messaging.templates;


import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.EmailMessageTemplate;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Thymeleaf renderer for the welcome email sent after account confirmation.
 * It maps NotificationMessage details into the HTML template stored in resources/templates/emails.
 */
public class UserWelcomeEmailTemplate implements EmailTemplate {

    private static final String EMAIL_TEMPLATES_PATH = "emails/";
    private final static String USER_WELCOME_TEMPLATE = EMAIL_TEMPLATES_PATH + "user-welcome-template";

    private final SpringTemplateEngine springTemplateEngine;

    public UserWelcomeEmailTemplate(SpringTemplateEngine springTemplateEngine) {
        this.springTemplateEngine = springTemplateEngine;
    }

    @Override
    public EmailMessageTemplate supports() {
        return EmailMessageTemplate.EMAIL_USER_SUCCESSFULLY_ACTIVATED;
    }

    @Override
    public String render(NotificationMessage notificationMessage) {

        Context context = new Context();
        context.setVariable("username", notificationMessage.recipient());
        notificationMessage.details().forEach(context::setVariable);

        return springTemplateEngine.process(USER_WELCOME_TEMPLATE, context);
    }

}
