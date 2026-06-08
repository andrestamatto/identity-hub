package br.dev.andrestamatto.identityhub.infrastructure.messaging.templates;


import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.EmailMessageTemplate;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

/**
 * Thymeleaf renderer for the user verification-code email.
 * It maps NotificationMessage details into the HTML template stored in resources/templates/emails.
 */
public class UserVerificationCodeEmailTemplate implements EmailTemplate {

    private static final String EMAIL_TEMPLATES_PATH = "emails/";
    private final static String USER_VERIFICATION_CODE_TEMPLATE = EMAIL_TEMPLATES_PATH + "user-verification-code";

    private final SpringTemplateEngine springTemplateEngine;

    public UserVerificationCodeEmailTemplate(SpringTemplateEngine springTemplateEngine) {
        this.springTemplateEngine = springTemplateEngine;
    }

    @Override
    public EmailMessageTemplate supports() {
        return EmailMessageTemplate.EMAIL_USER_VERIFICATION_CODE;
    }

    @Override
    public String render(NotificationMessage notificationMessage) {

        Context context = new Context();
        context.setVariable("username", notificationMessage.recipient());
        notificationMessage.details().forEach(context::setVariable);

        return springTemplateEngine.process(USER_VERIFICATION_CODE_TEMPLATE, context);
    }

    private void validate(String recipient,  Map<String, String> templateDetails) {
        if (recipient == null || recipient.isBlank()) {throw new IllegalArgumentException("Username cannot be null or blank");}
        if (templateDetails == null || templateDetails.isEmpty()) {throw new IllegalArgumentException("Template details cannot be null or empty");}
    }
}
