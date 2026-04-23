package br.dev.andrestamatto.identityhub.domain.service;

import br.dev.andrestamatto.identityhub.domain.model.User;
import br.dev.andrestamatto.identityhub.infrastructure.security.password.BCryptPasswordEncoderAdapter;

public class LoginProvider implements AuthProvider {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoderAdapter();

    @Override
    public User authenticate(String email, String password) {
        return new User(email, passwordEncoder.encode(password));
    }

}
