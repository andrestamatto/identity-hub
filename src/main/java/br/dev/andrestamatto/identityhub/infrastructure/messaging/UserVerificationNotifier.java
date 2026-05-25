package br.dev.andrestamatto.identityhub.infrastructure.messaging;

import br.dev.andrestamatto.identityhub.application.ports.output.UserNotifier;
import br.dev.andrestamatto.identityhub.domain.valueobjects.NotificationMethod;

public class UserVerificationNotifier implements UserNotifier {

    @Override
    public void notify(String who, Object what, NotificationMethod method) {

    }
}
