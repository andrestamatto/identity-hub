package br.dev.andrestamatto.identityhub.application.usecase.port.in;

import br.dev.andrestamatto.identityhub.application.result.AuthenticationResult;
import br.dev.andrestamatto.identityhub.domain.model.RawPassword;

public interface PasswordLoginUseCasePort {
    AuthenticationResult execute(String requestIdentity, RawPassword requestPassword);
}
