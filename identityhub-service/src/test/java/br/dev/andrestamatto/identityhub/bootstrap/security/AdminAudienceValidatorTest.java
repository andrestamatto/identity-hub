package br.dev.andrestamatto.identityhub.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class AdminAudienceValidatorTest {

    private static final String ADMIN_AUDIENCE = "identityhub-admin-api";

    @Test
    void acceptsOnlyTheConfiguredAdministrativeAudience() {
        var validator = new AdminAudienceValidator(ADMIN_AUDIENCE);

        assertThat(validator.validate(jwtWithAudience(ADMIN_AUDIENCE)).hasErrors()).isFalse();
        assertThat(validator.validate(jwtWithAudience("consumer-api")).hasErrors()).isTrue();
    }

    private Jwt jwtWithAudience(String audience) {
        var issuedAt = Instant.parse("2026-07-29T12:00:00Z");
        return new Jwt(
                "token-value",
                issuedAt,
                issuedAt.plusSeconds(300),
                java.util.Map.of("alg", "RS256"),
                java.util.Map.of("sub", "operator-id", "aud", List.of(audience)));
    }
}
