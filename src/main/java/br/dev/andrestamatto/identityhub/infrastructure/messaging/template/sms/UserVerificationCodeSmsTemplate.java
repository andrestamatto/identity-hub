package br.dev.andrestamatto.identityhub.infrastructure.messaging.template.sms;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.SmsMessageTemplate;

/**
 * Plain-text SMS template for user verification codes.
 * It intentionally keeps the message short to fit common SMS constraints.
 */
public class UserVerificationCodeSmsTemplate implements SmsTemplate {

    @Override
    public SmsMessageTemplate supports() {
        return SmsMessageTemplate.SMS_USER_VERIFICATION_CODE;
    }

    @Override
    public String render(NotificationMessage notificationMessage) {
        var details = notificationMessage.details();
        return "IdentityHub code: " + details.get("verificationCode") + ". Expires at " + details.get("expiresAt") + ".";
    }
}
