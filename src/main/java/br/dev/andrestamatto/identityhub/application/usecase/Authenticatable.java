package br.dev.andrestamatto.identityhub.application.usecase;

import br.dev.andrestamatto.identityhub.domain.model.Password;
import br.dev.andrestamatto.identityhub.interfaces.rest.dto.AuthenticatableResponse;

public interface Authenticatable {
    AuthenticatableResponse execute(String requestEmail, Password requestPassword);
}
