package br.dev.andrestamatto.identityhub.infrastructure.messaging;

import br.dev.andrestamatto.identityhub.application.events.UserConfirmedEvent;
import br.dev.andrestamatto.identityhub.application.events.UserRegisteredPendingVerificationEvent;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.UserNotifier;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.MessageTemplate;
import br.dev.andrestamatto.identityhub.domain.valueobjects.NotificationMethod;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
public class UserNotificationListeners {

    private final UserNotifier userNotifier;

    public UserNotificationListeners(UserNotifier userNotifier) {
        this.userNotifier = userNotifier;
    }

    /*
     *  UserRegisteredPendingVerificationEvent
     * */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserRegisteredPendingVerificationEvent event) {
        var notificationMethod = event.verificationToken().method();

        var notificationMessage = NotificationMessage.create(
                MessageTemplate.USER_VERIFICATION_CODE,
                event.username().value(),
                Map.of(
                        "subject", "Verify your identity",
                        "verificationCode", event.verificationToken().code(),
                        "expiresAt", String.valueOf(event.verificationToken().expiresAt())
                )
        );
        userNotifier.notify(notificationMessage, notificationMethod);

    }


    /*
    *  UserConfirmedEvent
    * */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserConfirmedEvent event) {
        var notificationMethod = switch (event.username().usernameType()) {
            case EMAIL -> NotificationMethod.EMAIL;
            case PHONE -> NotificationMethod.SMS;
            case EMAIL_OR_PHONE -> NotificationMethod.BOTH;
            case EXTERNAL_ID, UNKNOWN -> NotificationMethod.EMAIL;
        };

        var notificationMessage = NotificationMessage.create(
                MessageTemplate.USER_SUCCESSFULLY_ACTIVATED,
                event.username().value(),
                Map.of(
                        "subject", "Welcome to IdentityHub",
                        "message", "Your account has been confirmed successfully."
                )
        );
        userNotifier.notify(notificationMessage, notificationMethod);
    }


}
