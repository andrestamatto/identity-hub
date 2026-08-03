package br.dev.andrestamatto.identityhub.integration.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class IdentityHubAudienceValidatorTest {

    private final IdentityHubAudienceValidator validator = new IdentityHubAudienceValidator("catalog-api");

    @Test
    void acceptsOnlyTheConfiguredAudience() {
        assertThat(validator.validate(jwt(List.of("account-api", "catalog-api"))).hasErrors()).isFalse();
        assertThat(validator.validate(jwt(List.of("account-api"))).hasErrors()).isTrue();
        assertThat(validator.validate(jwt(List.of())).hasErrors()).isTrue();
    }

    private Jwt jwt(List<String> audience) {
        var issuedAt = Instant.parse("2026-08-03T12:00:00Z");
        return new Jwt(
                "synthetic-token",
                issuedAt,
                issuedAt.plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of("sub", "account-123", "aud", audience));
    }
}
