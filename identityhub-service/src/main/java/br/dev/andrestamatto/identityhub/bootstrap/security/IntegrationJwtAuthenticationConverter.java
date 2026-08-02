package br.dev.andrestamatto.identityhub.bootstrap.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

final class IntegrationJwtAuthenticationConverter
        implements Converter<Jwt, AbstractOAuth2TokenAuthenticationToken<Jwt>> {

    private static final String MACHINE_CLIENT_PREFIX = "ih-machine-";

    @Override
    public AbstractOAuth2TokenAuthenticationToken<Jwt> convert(Jwt token) {
        var machineClientId = machineClientId(token.getClaimAsString("azp"));
        return new JwtAuthenticationToken(token, authorities(token), machineClientId.toString());
    }

    private UUID machineClientId(String authorizedParty) {
        if (authorizedParty == null || !authorizedParty.startsWith(MACHINE_CLIENT_PREFIX)) {
            throw new BadJwtException("Token is not assigned to an IdentityHub machine client");
        }
        try {
            return UUID.fromString(authorizedParty.substring(MACHINE_CLIENT_PREFIX.length()));
        } catch (IllegalArgumentException exception) {
            throw new BadJwtException("Token contains an invalid machine client identity", exception);
        }
    }

    private Collection<GrantedAuthority> authorities(Jwt token) {
        var scope = token.getClaimAsString("scope");
        if (scope == null || scope.isBlank()) {
            return List.of();
        }
        return List.of(scope.trim().split("\\s+"))
                .stream()
                .map(value -> new SimpleGrantedAuthority("SCOPE_" + value))
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
