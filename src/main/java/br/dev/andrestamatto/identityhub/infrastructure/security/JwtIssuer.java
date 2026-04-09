package br.dev.andrestamatto.identityhub.infrastructure.security;

import br.dev.andrestamatto.identityhub.domain.model.User;

public class JwtIssuer implements TokenIssuer {

    private final String SECRET = "my-secret-key";

    @Override
    public String issue(User user) {
        return "";
    }
}
