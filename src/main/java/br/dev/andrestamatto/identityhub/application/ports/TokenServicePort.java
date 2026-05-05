package br.dev.andrestamatto.identityhub.application.ports;

import br.dev.andrestamatto.identityhub.domain.model.User;
import io.jsonwebtoken.Claims;

public interface TokenServicePort {
    String issue(User user);
    boolean isValid(String token);
    Claims extractClaims(String token);
    long accessTokenExpiresInSeconds();
}
