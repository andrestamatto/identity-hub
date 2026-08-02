package br.dev.andrestamatto.identityhub.identity.adapter.out.communication;

import br.dev.andrestamatto.identityhub.communication.application.RequestPasswordRecoveryEmail;
import br.dev.andrestamatto.identityhub.identity.application.RecoveryEmailRequester;
import java.util.Objects;

public final class CommunicationRecoveryEmailRequester implements RecoveryEmailRequester {

    private final RequestPasswordRecoveryEmail requestEmail;

    public CommunicationRecoveryEmailRequester(RequestPasswordRecoveryEmail requestEmail) {
        this.requestEmail = Objects.requireNonNull(requestEmail);
    }

    @Override
    public void request(Command command) {
        requestEmail.execute(new RequestPasswordRecoveryEmail.Command(
                command.challengeId(),
                command.applicationId(),
                command.recipient(),
                command.recoveryUrl(),
                command.correlationId()));
    }
}
