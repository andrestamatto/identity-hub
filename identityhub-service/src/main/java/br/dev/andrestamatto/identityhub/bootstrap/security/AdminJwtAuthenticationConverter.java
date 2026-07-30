package br.dev.andrestamatto.identityhub.bootstrap.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

final class AdminJwtAuthenticationConverter
        implements Converter<Jwt, AbstractOAuth2TokenAuthenticationToken<Jwt>> {

    private static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
    private static final String PLATFORM_AUDITOR = "PLATFORM_AUDITOR";

    @Override
    public AbstractOAuth2TokenAuthenticationToken<Jwt> convert(Jwt token) {
        return new JwtAuthenticationToken(token, authorities(token), token.getSubject());
    }

    private Collection<GrantedAuthority> authorities(Jwt token) {
        var authorities = new ArrayList<GrantedAuthority>();
        platformRoles(token).stream()
                .filter(role -> PLATFORM_ADMIN.equals(role) || PLATFORM_AUDITOR.equals(role))
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .forEach(authorities::add);

        if (authenticationMethods(token).contains("totp")) {
            authorities.add(new SimpleGrantedAuthority("MFA_TOTP"));
        }
        return List.copyOf(authorities);
    }

    private List<String> platformRoles(Jwt token) {
        var realmAccess = token.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return List.of();
        }
        return strings(realmAccess.get("roles"));
    }

    private List<String> authenticationMethods(Jwt token) {
        return strings(token.getClaims().get("amr"));
    }

    private List<String> strings(Object claim) {
        if (!(claim instanceof Collection<?> values)) {
            return List.of();
        }
        return values.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }
}
