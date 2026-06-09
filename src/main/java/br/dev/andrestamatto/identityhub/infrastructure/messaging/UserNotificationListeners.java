package br.dev.andrestamatto.identityhub.infrastructure.messaging;

import br.dev.andrestamatto.identityhub.application.events.UserConfirmedEvent;
import br.dev.andrestamatto.identityhub.application.events.UserRegisteredPendingVerificationEvent;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.channels.NotificationChannels;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.notifiers.UserNotifier;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.EmailMessageTemplate;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.MessageTemplates;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.SmsMessageTemplate;
import br.dev.andrestamatto.identityhub.domain.valueobjects.NotificationMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Spring event listener responsible for translating application events into
 * NotificationMessage objects. It runs after transaction commit so notifications
 * are only attempted after the user state has been persisted.
 */
@Component
public class UserNotificationListeners {

    private static final DateTimeFormatter EMAIL_DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final UserNotifier userNotifier;

    public UserNotificationListeners(UserNotifier userNotifier) {
        this.userNotifier = userNotifier;
    }

    /*
     *  UserRegisteredPendingVerificationEvent
     * */
    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserRegisteredPendingVerificationEvent event) {
        var notificationMethod = event.verificationToken().method();

        var messageTemplates = new MessageTemplates(
                EmailMessageTemplate.EMAIL_USER_VERIFICATION_CODE,
                SmsMessageTemplate.UNDEFINED
        );

        var notificationMessage = NotificationMessage.create(
                event.username().value(),
                Map.of(
                        "subject", "Verify your identity",
                        "verificationCode", event.verificationToken().code(),
                        "expiresAt", formatForEmail(event.verificationToken().expiresAt())
                ),
                messageTemplates,
                notificationChannelsFrom(notificationMethod)
        );
        userNotifier.notify(notificationMessage, notificationMethod);

    }


    /*
    *  UserConfirmedEvent
    * */
    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserConfirmedEvent event) {
        var notificationMethod = switch (event.username().usernameType()) {
            case EMAIL -> NotificationMethod.EMAIL;
            case PHONE -> NotificationMethod.SMS;
            case EMAIL_OR_PHONE -> NotificationMethod.BOTH;
            case EXTERNAL_ID, UNKNOWN -> NotificationMethod.EMAIL;
        };

        var messageTemplates = new MessageTemplates(
                EmailMessageTemplate.EMAIL_USER_SUCCESSFULLY_ACTIVATED,
                SmsMessageTemplate.UNDEFINED
        );

        var notificationMessage = NotificationMessage.create(
                event.username().value(),
                Map.of(
                        "subject", "Welcome to IdentityHub",
                        "message", "Your account has been confirmed successfully."
                ),
                messageTemplates,
                notificationChannelsFrom(notificationMethod)
        );
        userNotifier.notify(notificationMessage, notificationMethod);
    }

    private NotificationChannels notificationChannelsFrom(NotificationMethod notificationMethod) {
        return switch (notificationMethod) {
            case EMAIL -> NotificationChannels.email();
            case SMS -> NotificationChannels.sms();
            case BOTH -> NotificationChannels.emailAndSms();
        };
    }

    private String formatForEmail(Instant instant) {
        return EMAIL_DATE_TIME_FORMATTER.format(instant);
    }

}
