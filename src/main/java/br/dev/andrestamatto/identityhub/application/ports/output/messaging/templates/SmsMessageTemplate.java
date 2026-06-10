package br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates;

/**
 * SMS template identifiers understood by the notification flow.
 * SMS rendering is not implemented yet, but the enum reserves the contract.
 */
public enum SmsMessageTemplate {
    UNDEFINED("undefined"),
    SMS_USER_VERIFICATION_CODE("sms-user-verification-code"),
    SMS_USER_SUCCESSFULLY_ACTIVATED("sms-user-successfully-activated");

    private final String value;

    SmsMessageTemplate(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

}
