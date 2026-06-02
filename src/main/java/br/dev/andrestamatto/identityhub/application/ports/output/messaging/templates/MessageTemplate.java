package br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates;

public enum MessageTemplate {
    USER_VERIFICATION_CODE("verification-code"),
    USER_SUCCESSFULLY_ACTIVATED("user-successfully-activated");

    private String value;

    MessageTemplate(String value) {}

}
