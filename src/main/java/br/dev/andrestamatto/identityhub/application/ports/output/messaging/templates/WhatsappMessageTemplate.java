package br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates;

/**
 * WhatsApp template identifiers understood by the notification flow.
 */
public enum WhatsappMessageTemplate {
    UNDEFINED("undefined"),
    WHATSAPP_USER_VERIFICATION_CODE("whatsapp-user-verification-code"),
    WHATSAPP_USER_SUCCESSFULLY_ACTIVATED("whatsapp-user-successfully-activated");

    private final String value;

    WhatsappMessageTemplate(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

}
