package br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates;

/**
 * Email template identifiers understood by the notification flow.
 * Each value is resolved by infrastructure to a concrete template renderer.
 */
public enum EmailMessageTemplate {
    UNDEFINED("undefined"),
    EMAIL_USER_VERIFICATION_CODE("email-user-verification-code"),
    EMAIL_USER_SUCCESSFULLY_ACTIVATED("email-user-successfully-activated");

    private String value;

    EmailMessageTemplate(String value) {}

}
