package br.dev.andrestamatto.identityhub.domain.service;

import br.dev.andrestamatto.identityhub.domain.model.User;

public class LoginProvider implements AuthProvider {

    @Override
    public User authenticate(String email, String password) {
        return new User(email, password);
    }
}
