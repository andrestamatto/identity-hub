package br.dev.andrestamatto.identityhub.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class IntegrationAudienceValidatorTest {

    private final IntegrationAudienceValidator validator = new IntegrationAudienceValidator();

    @Test
    void acceptsOnlyTheStableIntegrationAudience() {
        assertThat(validator.validate(jwt("identityhub-integration-api")).hasErrors()).isFalse();
        assertThat(validator.validate(jwt("another-api")).hasErrors()).isTrue();
    }

    private Jwt jwt(String audience) {
        return new Jwt(
                "synthetic-token",
                Instant.parse("2026-08-02T18:00:00Z"),
                Instant.parse("2026-08-02T18:05:00Z"),
                Map.of("alg", "RS256"),
                Map.of("sub", "machine-client", "aud", List.of(audience)));
    }
}
