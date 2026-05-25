package br.dev.andrestamatto.identityhub.application.ports.output;

import br.dev.andrestamatto.identityhub.domain.valueobjects.NotificationMethod;

public interface UserNotifier {
    void notify(String who, Object what, NotificationMethod method);
}
