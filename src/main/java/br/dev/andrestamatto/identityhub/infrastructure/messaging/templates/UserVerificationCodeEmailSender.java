package br.dev.andrestamatto.identityhub.infrastructure.messaging.templates;


import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

public class ConfirmationCodeEmailTemplate implements EmailTemplate {

    private static final String EMAIL_TEMPLATES_PATH = "emails/";
    private final static String USER_VERIFICATION_CODE_TEMPLATE = EMAIL_TEMPLATES_PATH + "user-verification-code";

    private final SpringTemplateEngine springTemplateEngine;

    public ConfirmationCodeEmailTemplate(SpringTemplateEngine springTemplateEngine) {
        this.springTemplateEngine = springTemplateEngine;
    }

    public String create(String recipient, Map<String, String> templateDetails) {

        validate(recipient, templateDetails);

        Context context = new Context();
        context.setVariable("recipient", recipient);
        templateDetails.forEach(context::setVariable);

        return springTemplateEngine.process(USER_VERIFICATION_CODE_TEMPLATE, context);
    }

    private void validate(String recipient,  Map<String, String> templateDetails) {
        if (recipient == null || recipient.isBlank()) {throw new IllegalArgumentException("Username cannot be null or blank");}
        if (templateDetails == null || templateDetails.isEmpty()) {throw new IllegalArgumentException("Template details cannot be null or empty");}
    }
}
