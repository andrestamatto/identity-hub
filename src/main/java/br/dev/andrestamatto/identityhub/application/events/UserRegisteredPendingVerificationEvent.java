package br.dev.andrestamatto.identityhub.application.events;

import br.dev.andrestamatto.identityhub.domain.valueobjects.NotificationMethod;
import br.dev.andrestamatto.identityhub.domain.valueobjects.Username;

public record UserRegisteredPendingVerificationEvent(
    Username username,
    String verificationCode,
    NotificationMethod method
) {

    public UserRegisteredPendingVerificationEvent {
        if ( username == null ) {throw new IllegalArgumentException("username cannot be null");}
        if ( verificationCode == null || verificationCode.isBlank()) { throw new IllegalArgumentException("Verification code cannot be null or blank"); }
        if ( method == null ) { throw new IllegalArgumentException("method cannot be null"); }
    }
}
