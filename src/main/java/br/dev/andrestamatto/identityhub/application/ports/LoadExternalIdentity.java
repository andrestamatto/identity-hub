package br.dev.andrestamatto.identityhub.application.ports;

import br.dev.andrestamatto.identityhub.domain.model.ExternalUser;

import java.util.Optional;

public interface LoadExternalIdentity {
    Optional<ExternalUser> findByEmail(String emailValue);
}
