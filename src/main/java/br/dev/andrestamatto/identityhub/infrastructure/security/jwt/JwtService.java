package br.dev.andrestamatto.identityhub.infrastructure.security.jwt;

import br.dev.andrestamatto.identityhub.domain.model.PermissionName;
import br.dev.andrestamatto.identityhub.domain.model.RoleName;
import br.dev.andrestamatto.identityhub.domain.model.User;
import br.dev.andrestamatto.identityhub.application.ports.TokenServicePort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtService implements TokenServicePort {

    private final JwtProperties properties;
    private final SecretKey key;
    private final String defaultIdentityType;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.defaultIdentityType = normalize(properties.defaultIdentityType());
        validateIdentityTypeConfiguration();
    }

    @Override
    public String issue(User user) {
        var now = Instant.now();
        var expiration = now.plusSeconds(properties.accessTokenExpirationSeconds());
        var tokenBuilder = Jwts.builder()
                .subject(user.getId().toString())
                .claim("identity", user.getIdentity())
                .claim("identity_type", defaultIdentityType)
                .claim("roles", user.getRoles().stream().map(RoleName::value).collect(Collectors.toUnmodifiableSet()))
                .claim("permissions", user.getPermissions().stream().map(PermissionName::value).collect(Collectors.toUnmodifiableSet()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration));

        return tokenBuilder
                .signWith(key)
                .compact();
    }

    @Override
    public boolean isValid(String token) {
        return extractClaims(token) != null;
    }

    @Override
    public Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public long accessTokenExpiresInSeconds() {
        return properties.accessTokenExpirationSeconds();
    }

    private void validateIdentityTypeConfiguration() {
        if (defaultIdentityType.isBlank()) {
            throw new IllegalStateException("identity-hub.jwt.default-identity-type must not be blank");
        }

        Set<String> supported = properties.supportedIdentityTypes() == null
                ? Set.of()
                : properties.supportedIdentityTypes().stream()
                .map(this::normalize)
                .collect(Collectors.toUnmodifiableSet());

        if (!supported.isEmpty() && !supported.contains(defaultIdentityType)) {
            throw new IllegalStateException(
                    "identity-hub.jwt.default-identity-type must be present in identity-hub.jwt.supported-identity-types"
            );
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
