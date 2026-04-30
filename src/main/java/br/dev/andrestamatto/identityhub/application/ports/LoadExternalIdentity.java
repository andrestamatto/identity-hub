package br.dev.andrestamatto.identityhub.application.ports;

import br.dev.andrestamatto.identityhub.domain.model.User;

import java.util.Optional;

public interface LoadExternalIdentity {
    Optional<User> load(String identityValue);
}
