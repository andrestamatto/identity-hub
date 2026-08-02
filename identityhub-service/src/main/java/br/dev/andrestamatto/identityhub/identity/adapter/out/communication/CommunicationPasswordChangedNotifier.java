package br.dev.andrestamatto.identityhub.identity.adapter.out.communication;

import br.dev.andrestamatto.identityhub.communication.application.RequestPasswordChangedEmail;
import br.dev.andrestamatto.identityhub.identity.application.PasswordChangedNotifier;
import java.util.Objects;

public final class CommunicationPasswordChangedNotifier implements PasswordChangedNotifier {

    private final RequestPasswordChangedEmail requestEmail;

    public CommunicationPasswordChangedNotifier(RequestPasswordChangedEmail requestEmail) {
        this.requestEmail = Objects.requireNonNull(requestEmail);
    }

    @Override
    public void notify(Command command) {
        requestEmail.execute(new RequestPasswordChangedEmail.Command(
                command.deliveryId(),
                command.applicationId(),
                command.recipient(),
                command.correlationId()));
    }
}
