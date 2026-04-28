package br.dev.andrestamatto.identityhub.domain.service;

import br.dev.andrestamatto.identityhub.application.ports.LoadExternalIdentity;
import br.dev.andrestamatto.identityhub.domain.model.RawPassword;
import br.dev.andrestamatto.identityhub.domain.model.User;

public class LoginProvider implements AuthProvider {

    private final PasswordEncoder passwordEncoder;
    private final LoadExternalIdentity externalIdentity;

    public LoginProvider(PasswordEncoder passwordEncoder, LoadExternalIdentity externalIdentity) {
        this.passwordEncoder = passwordEncoder;
        this.externalIdentity = externalIdentity;
    }

    @Override
    public User authenticate(String identity, RawPassword rawPassword) {
        return externalIdentity.findByIdentity(identity)
                .filter(user -> user.getId() != null)
                .filter(user -> passwordEncoder.matches(
                        rawPassword, user.getEncodedPassword()
                ))
                .orElse(null);
    }

}
