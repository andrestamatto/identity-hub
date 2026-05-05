package br.dev.andrestamatto.identityhub.domain.service;

import br.dev.andrestamatto.identityhub.application.ports.LoadExternalIdentityPort;
import br.dev.andrestamatto.identityhub.domain.model.RawPassword;
import br.dev.andrestamatto.identityhub.domain.model.User;

public class PasswordLoginAuthenticatorService implements PasswordLoginAuthenticator {

    private final PasswordEncoder passwordEncoder;
    private final LoadExternalIdentityPort externalIdentity;

    public PasswordLoginAuthenticatorService(PasswordEncoder passwordEncoder, LoadExternalIdentityPort externalIdentity) {
        this.passwordEncoder = passwordEncoder;
        this.externalIdentity = externalIdentity;
    }

    @Override
    public User authenticate(String identity, RawPassword rawPassword) {
        return externalIdentity.load(identity)
                .filter(user -> user.getId() != null)
                .filter(user -> passwordEncoder.matches(
                        rawPassword, user.getEncodedPassword()
                ))
                .orElse(null);
    }

}
