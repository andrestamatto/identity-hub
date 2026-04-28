package br.dev.andrestamatto.identityhub.infrastructure.security.jwt;

import br.dev.andrestamatto.identityhub.domain.model.EncodedPassword;
import br.dev.andrestamatto.identityhub.domain.model.PermissionName;
import br.dev.andrestamatto.identityhub.domain.model.RoleName;
import br.dev.andrestamatto.identityhub.domain.model.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    @Test
    void shouldIssueAndParseTokenWithExpectedClaims() {
        var properties = new JwtProperties(
                "8fd334bf75108aeb63c2bf1968c13e1d78df69a170d2d41496e524c3947078d1",
                3600,
                "email",
                List.of("email", "username")
        );
        var service = new JwtService(properties);

        var user = new User(
                UUID.randomUUID(),
                "user@identityhub.dev",
                EncodedPassword.from("$2a$10$abc"),
                Set.of(RoleName.from("admin")),
                Set.of(PermissionName.from("report_read"))
        );

        var token = service.issue(user);
        Claims claims = service.extractClaims(token);

        assertNotNull(claims);
        assertEquals(user.getId().toString(), claims.getSubject());
        assertEquals("user@identityhub.dev", claims.get("identity", String.class));
        assertEquals("email", claims.get("identity_type", String.class));
        assertTrue(claims.get("roles", List.class).contains("ADMIN"));
        assertTrue(claims.get("permissions", List.class).contains("REPORT_READ"));
        assertTrue(service.isValid(token));
    }

    @Test
    void shouldReturnInvalidForMalformedToken() {
        var properties = new JwtProperties(
                "8fd334bf75108aeb63c2bf1968c13e1d78df69a170d2d41496e524c3947078d1",
                3600,
                "email",
                List.of("email")
        );
        var service = new JwtService(properties);

        assertNull(service.extractClaims("not-a-jwt"));
        assertFalse(service.isValid("not-a-jwt"));
    }

    @Test
    void shouldFailFastWhenDefaultIdentityTypeIsNotSupported() {
        var properties = new JwtProperties(
                "8fd334bf75108aeb63c2bf1968c13e1d78df69a170d2d41496e524c3947078d1",
                3600,
                "cpf",
                List.of("email", "username")
        );

        var ex = assertThrows(IllegalStateException.class, () -> new JwtService(properties));
        assertTrue(ex.getMessage().contains("default-identity-type"));
    }
}
