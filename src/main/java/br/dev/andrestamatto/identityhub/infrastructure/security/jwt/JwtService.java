package br.dev.andrestamatto.identityhub.infrastructure.security.jwt;

import br.dev.andrestamatto.identityhub.domain.model.User;
import br.dev.andrestamatto.identityhub.infrastructure.security.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtService implements TokenService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String issue(User user) {
        var now = Instant.now();
        var expiration = now.plusSeconds(properties.accessTokenExpirationSeconds());

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", user.getRoles())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
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
}
