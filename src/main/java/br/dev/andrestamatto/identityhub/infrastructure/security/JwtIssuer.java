package br.dev.andrestamatto.identityhub.infrastructure.security;

import br.dev.andrestamatto.identityhub.domain.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtIssuer implements TokenIssuer {

    private final String SECRET = "8fd334bf75108aeb63c2bf1968c13e1d78df69a170d2d41496e524c3947078d1";

    @Override
    public String issue(User user) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(user.id.toString())
                .claim("email", user.email)
                .claim("roles", user.roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000)) // 1 day
                .signWith(key)
                .compact();
    }
}
