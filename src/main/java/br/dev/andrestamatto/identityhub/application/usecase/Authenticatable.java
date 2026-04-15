package br.dev.andrestamatto.identityhub.application.usecase;

import br.dev.andrestamatto.identityhub.interfaces.rest.dto.AuthenticatableResponse;

public interface Authenticatable {
    AuthenticatableResponse execute(String email, String password);
}
