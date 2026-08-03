package br.dev.andrestamatto.identityhub.integration.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class IdentityHubPublicClaimsValidatorTest {

    private static final Instant NOW = Instant.parse("2026-08-03T12:00:00Z");
    private final IdentityHubPublicClaimsValidator validator = new IdentityHubPublicClaimsValidator(
            Clock.fixed(NOW, ZoneOffset.UTC),
            Duration.ofSeconds(60));

    @Test
    void acceptsTheDocumentedPublicClaims() {
        assertThat(validator.validate(jwt(
                NOW,
                "account-123",
                "token-123",
                "catalog:read catalog:write",
                List.of("catalog-reader"))).hasErrors()).isFalse();
    }

    @Test
    void rejectsMissingOrFutureRequiredClaims() {
        assertThat(validator.validate(jwt(null, "account-123", "token-123", "catalog:read", List.of())).hasErrors())
                .isTrue();
        assertThat(validator.validate(jwt(NOW, null, "token-123", "catalog:read", List.of())).hasErrors())
                .isTrue();
        assertThat(validator.validate(jwt(NOW, "account-123", null, "catalog:read", List.of())).hasErrors())
                .isTrue();
        assertThat(validator.validate(jwt(NOW, "account-123", "token-123", null, List.of())).hasErrors())
                .isTrue();
        assertThat(validator.validate(jwt(NOW, "account-123", "token-123", "catalog:read", null)).hasErrors())
                .isTrue();
        assertThat(validator.validate(jwt(NOW.plusSeconds(61), "account-123", "token-123", "catalog:read", List.of()))
                .hasErrors()).isTrue();
    }

    @Test
    void rejectsMalformedDuplicateExcessiveAndPlatformAuthorities() {
        assertThat(validator.validate(jwt(NOW, "account-123", "token-123", "catalog:read catalog:read", List.of()))
                .hasErrors()).isTrue();
        assertThat(validator.validate(jwt(NOW, "account-123", "token-123", "catalog:read", List.of("not valid")))
                .hasErrors()).isTrue();
        assertThat(validator.validate(jwt(NOW, "account-123", "token-123", "catalog:read", List.of("PLATFORM_ADMIN")))
                .hasErrors()).isTrue();
        assertThat(validator.validate(jwt(
                NOW,
                "account-123",
                "token-123",
                "catalog:read",
                new ArrayList<>(java.util.Collections.nCopies(101, "catalog-reader")))).hasErrors()).isTrue();
    }

    private Jwt jwt(
            Instant issuedAt,
            String subject,
            String tokenId,
            String scope,
            List<String> roles) {
        var claims = new java.util.HashMap<String, Object>();
        claims.put("iss", "https://auth.example.test/realms/identityhub");
        claims.put("aud", List.of("catalog-api"));
        if (subject != null) {
            claims.put("sub", subject);
        }
        if (tokenId != null) {
            claims.put("jti", tokenId);
        }
        if (scope != null) {
            claims.put("scope", scope);
        }
        if (roles != null) {
            claims.put("roles", roles);
        }
        return new Jwt(
                "synthetic-token",
                issuedAt,
                NOW.plusSeconds(300),
                Map.of("alg", "RS256"),
                claims);
    }
}
