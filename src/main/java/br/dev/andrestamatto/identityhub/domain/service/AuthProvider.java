package br.dev.andrestamatto.identityhub.domain.service;

import br.dev.andrestamatto.identityhub.domain.model.Password;
import br.dev.andrestamatto.identityhub.domain.model.User;

public interface AuthProvider {
    User authenticate(String email, Password password);
}
