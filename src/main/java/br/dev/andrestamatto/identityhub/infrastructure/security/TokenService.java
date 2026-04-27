package br.dev.andrestamatto.identityhub.infrastructure.security;

import br.dev.andrestamatto.identityhub.domain.model.User;
import io.jsonwebtoken.Claims;

public interface TokenService {
    String issue(User user);
    boolean isValid(String token);
    Claims extractClaims(String token);
    long accessTokenExpiresInSeconds();
}
