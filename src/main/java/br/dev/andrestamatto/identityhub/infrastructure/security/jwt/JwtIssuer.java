package br.dev.andrestamatto.identityhub.infrastructure.security.jwt;

import br.dev.andrestamatto.identityhub.domain.model.User;
import br.dev.andrestamatto.identityhub.infrastructure.security.TokenIssuer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtIssuer implements TokenIssuer {



    private final String SECRET = "8fd334bf75108aeb63c2bf1968c13e1d78df69a170d2d41496e524c3947078d1";
    private SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    @Override
    public String issue(User user) {


        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", user.getRoles())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000)) // 1 day
                .signWith(key)
                .compact();

    }

    public boolean isValid(String token) {
        return (extractClaims(token) != null);
    }

    public Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return null;
    }

}
