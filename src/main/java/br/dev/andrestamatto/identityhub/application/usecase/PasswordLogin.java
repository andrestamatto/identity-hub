package br.dev.andrestamatto.identityhub.application.usecase;

import br.dev.andrestamatto.identityhub.application.result.AuthenticationResult;
import br.dev.andrestamatto.identityhub.domain.model.RawPassword;

public interface PasswordLogin {
    AuthenticationResult execute(String requestIdentity, RawPassword requestPassword);
}
