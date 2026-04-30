package br.dev.andrestamatto.identityhub.domain.service;

import br.dev.andrestamatto.identityhub.domain.model.RawPassword;
import br.dev.andrestamatto.identityhub.domain.model.User;

public interface PasswordLoginAuthenticator {
    User authenticate(String identity, RawPassword rawPassword);
}
