package br.dev.andrestamatto.identityhub.infrastructure.messaging;

import br.dev.andrestamatto.identityhub.application.events.UserConfirmedEvent;
import br.dev.andrestamatto.identityhub.application.events.UserRegisteredPendingVerificationEvent;
import br.dev.andrestamatto.identityhub.application.ports.output.UserNotifier;
import br.dev.andrestamatto.identityhub.domain.valueobjects.NotificationMethod;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UserNotificationListeners {

    private final UserNotifier userNotifier;

    public UserNotificationListeners(UserNotifier userNotifier) {
        this.userNotifier = userNotifier;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserRegisteredPendingVerificationEvent event) {
        userNotifier.notify(event.username().value(), event.verificationCode(), event.method());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserConfirmedEvent event) {
        var message = "Welcome to identityhub!";

        var method = switch (event.username().usernameType()) {
            case EMAIL -> NotificationMethod.EMAIL;
            case PHONE -> NotificationMethod.SMS;
            case EXTERNAL_ID -> throw new UnsupportedOperationException("Not supported yet.");
            case UNKNOWN -> throw new IllegalArgumentException("Cannot determine username type.");
        };

        userNotifier.notify(event.username().value(), message, method);
    }

}
