package br.dev.andrestamatto.identityhub.infrastructure.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "identity-hub.jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpirationSeconds
) {
}
