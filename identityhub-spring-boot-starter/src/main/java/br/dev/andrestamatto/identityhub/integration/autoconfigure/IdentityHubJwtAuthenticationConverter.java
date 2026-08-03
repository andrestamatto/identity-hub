package br.dev.andrestamatto.identityhub.integration.autoconfigure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

final class IdentityHubJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final IdentityHubSecurityProperties.Authorities authorities;

    IdentityHubJwtAuthenticationConverter(IdentityHubSecurityProperties.Authorities authorities) {
        this.authorities = authorities;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt token) {
        return new JwtAuthenticationToken(token, authorities(token), token.getSubject());
    }

    private Collection<GrantedAuthority> authorities(Jwt token) {
        var grantedAuthorities = new ArrayList<GrantedAuthority>();
        IdentityHubPublicAuthorityClaims.scopes(token.getClaims().get("scope"))
                .orElseGet(List::of)
                .stream()
                .map(scope -> new SimpleGrantedAuthority(authorities.scopePrefix() + scope))
                .forEach(grantedAuthorities::add);
        IdentityHubPublicAuthorityClaims.roles(token.getClaims().get("roles"))
                .orElseGet(List::of)
                .stream()
                .map(role -> new SimpleGrantedAuthority(authorities.rolePrefix() + role))
                .forEach(grantedAuthorities::add);
        return List.copyOf(grantedAuthorities);
    }
}
