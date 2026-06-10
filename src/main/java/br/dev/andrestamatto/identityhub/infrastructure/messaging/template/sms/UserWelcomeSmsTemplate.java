package br.dev.andrestamatto.identityhub.infrastructure.messaging.template.sms;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.SmsMessageTemplate;

/**
 * Plain-text SMS template for the welcome notification sent after confirmation.
 */
public class UserWelcomeSmsTemplate implements SmsTemplate {

    @Override
    public SmsMessageTemplate supports() {
        return SmsMessageTemplate.SMS_USER_SUCCESSFULLY_ACTIVATED;
    }

    @Override
    public String render(NotificationMessage notificationMessage) {
        return "Welcome to IdentityHub. Your account is active.";
    }
}
