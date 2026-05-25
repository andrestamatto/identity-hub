package br.dev.andrestamatto.identityhub.application.events;

import br.dev.andrestamatto.identityhub.domain.valueobjects.Username;

public record UserConfirmedEvent(
    Username username
) {

    public UserConfirmedEvent {
        if ( username == null ) {throw new IllegalArgumentException("username cannot be null");}
    }
}
