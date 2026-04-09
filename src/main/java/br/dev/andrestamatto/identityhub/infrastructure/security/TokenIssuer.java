package br.dev.andrestamatto.identityhub.infrastructure.security;

import br.dev.andrestamatto.identityhub.domain.model.User;

public interface TokenIssuer {
    String issue(User user);
}
