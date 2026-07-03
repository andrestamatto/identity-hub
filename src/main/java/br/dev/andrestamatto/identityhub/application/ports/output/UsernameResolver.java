package br.dev.andrestamatto.identityhub.application.ports.output;

import br.dev.andrestamatto.identityhub.domain.valueobjects.Username;

public interface UsernameResolver {
    Username resolve(String rawUsername);
}
