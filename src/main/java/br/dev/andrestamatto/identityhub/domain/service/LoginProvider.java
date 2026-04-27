package br.dev.andrestamatto.identityhub.domain.service;

import br.dev.andrestamatto.identityhub.application.ports.LoadExternalIdentity;
import br.dev.andrestamatto.identityhub.domain.model.Password;
import br.dev.andrestamatto.identityhub.domain.model.User;
import br.dev.andrestamatto.identityhub.infrastructure.mappers.UserMapper;

public class LoginProvider implements AuthProvider {

    private final PasswordEncoder passwordEncoder;
    private final LoadExternalIdentity externalIdentity;
    private final UserMapper userMapper;

    public LoginProvider(PasswordEncoder passwordEncoder, LoadExternalIdentity externalIdentity, UserMapper userMapper) {
        this.passwordEncoder = passwordEncoder;
        this.externalIdentity = externalIdentity;
        this.userMapper = userMapper;
    }

    @Override
    public User authenticate(String email, Password password) {
        return externalIdentity.findByEmail(email)
                .filter(externalUser -> externalUser.userId() != null)
                .filter(externalUser -> passwordEncoder.matches(
                        password.getValue(), externalUser.encodedPassword()
                ))
                .map(userMapper::toUser)
                .orElse(null);
    }

}
