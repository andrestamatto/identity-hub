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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(UserNotificationListeners.class);

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
        log.info(
                "Handling pending verification notification event. usernameType={} notificationMethod={}",
                event.username().usernameType(),
                notificationMethod
        );

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
        log.debug(
                "Dispatching verification notification. usernameType={} channels={}",
                event.username().usernameType(),
                notificationMessage.notificationChannels().values()
        );
        notifyUser(notificationMessage, notificationMethod);

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
        log.info(
                "Handling user confirmed notification event. usernameType={} notificationMethod={}",
                event.username().usernameType(),
                notificationMethod
        );

        var messageTemplates = new MessageTemplates(
                EmailMessageTemplate.EMAIL_USER_SUCCESSFULLY_ACTIVATED,
                SmsMessageTemplate.UNDEFINED
        );

        var notificationMessage = NotificationMessage.create(
                event.username().value(),
                Map.of(
                        "subject", "Welcome to IdentityHub"
                ),
                messageTemplates,
                notificationChannelsFrom(notificationMethod)
        );
        log.debug(
                "Dispatching welcome notification. usernameType={} channels={}",
                event.username().usernameType(),
                notificationMessage.notificationChannels().values()
        );
        notifyUser(notificationMessage, notificationMethod);
    }

    private void notifyUser(NotificationMessage notificationMessage, NotificationMethod notificationMethod) {
        try {
            userNotifier.notify(notificationMessage, notificationMethod);
        } catch (RuntimeException exception) {
            log.error(
                    "User notification failed. notificationMethod={} channels={} reason={}",
                    notificationMethod,
                    notificationMessage.notificationChannels().values(),
                    exception.getMessage(),
                    exception
            );
        }
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
