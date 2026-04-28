package br.dev.andrestamatto.identityhub.application.usecase;

import br.dev.andrestamatto.identityhub.domain.model.RawPassword;
import br.dev.andrestamatto.identityhub.interfaces.rest.dto.AuthenticatableResponse;

public interface Authenticatable {
    AuthenticatableResponse execute(String requestIdentity, RawPassword requestPassword);
}
