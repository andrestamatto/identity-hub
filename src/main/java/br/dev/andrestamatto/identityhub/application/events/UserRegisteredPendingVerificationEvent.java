package br.dev.andrestamatto.identityhub.application.events;

import br.dev.andrestamatto.identityhub.domain.valueobjects.Username;
import br.dev.andrestamatto.identityhub.domain.valueobjects.VerificationToken;

public record UserRegisteredPendingVerificationEvent(
    Username username,
    VerificationToken verificationToken
) {

    public UserRegisteredPendingVerificationEvent {
        if ( username == null ) {throw new IllegalArgumentException("username cannot be null");}
        if ( verificationToken == null ) { throw new IllegalArgumentException("verificationToken cannot be null"); }
    }
}
