package br.dev.andrestamatto.identityhub.identity.adapter.out.communication;

import br.dev.andrestamatto.identityhub.communication.application.RequestEmailVerificationEmail;
import br.dev.andrestamatto.identityhub.identity.application.VerificationEmailRequester;
import java.util.Objects;

public final class CommunicationVerificationEmailRequester
        implements VerificationEmailRequester {

    private final RequestEmailVerificationEmail requestEmail;

    public CommunicationVerificationEmailRequester(
            RequestEmailVerificationEmail requestEmail) {
        this.requestEmail = Objects.requireNonNull(requestEmail);
    }

    @Override
    public void request(Command command) {
        requestEmail.execute(new RequestEmailVerificationEmail.Command(
                command.deliveryId(),
                command.applicationId(),
                command.recipient(),
                command.verificationUrl(),
                command.correlationId()));
    }
}
