package br.dev.andrestamatto.identityhub.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class IntegrationAudienceValidatorTest {

    private final IntegrationAudienceValidator validator =
            new IntegrationAudienceValidator("identityhub-integration-api");

    @Test
    void acceptsOnlyExactIntegrationAudience() {
        assertThat(validator.validate(jwt("identityhub-integration-api")).hasErrors()).isFalse();
        assertThat(validator.validate(jwt("identityhub-admin-api")).hasErrors()).isTrue();
        assertThat(validator.validate(jwt("identityhub-integration-api-extra")).hasErrors())
                .isTrue();
        assertThat(validator.validate(jwt(
                        "identityhub-integration-api", "identityhub-admin-api")).hasErrors())
                .isTrue();
    }

    private Jwt jwt(String... audience) {
        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                java.util.Map.of("alg", "RS256"),
                java.util.Map.of("sub", "machine", "aud", List.of(audience)));
    }
}
