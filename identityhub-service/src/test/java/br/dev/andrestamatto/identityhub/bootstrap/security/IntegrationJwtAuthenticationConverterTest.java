package br.dev.andrestamatto.identityhub.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;

class IntegrationJwtAuthenticationConverterTest {

    private static final UUID CLIENT_ID =
            UUID.fromString("72c43df3-9f34-4dc6-85cc-5d323762f299");
    private final IntegrationJwtAuthenticationConverter converter =
            new IntegrationJwtAuthenticationConverter();

    @Test
    void derivesMachineIdentityAndStandardScopeAuthority() {
        var authentication = converter.convert(jwt(
                "ih-machine-" + CLIENT_ID,
                "onboarding:write unrelated:read"));

        assertThat(authentication.getName()).isEqualTo(CLIENT_ID.toString());
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder(
                        "SCOPE_onboarding:write", "SCOPE_unrelated:read");
    }

    @Test
    void rejectsNonMachineOrMalformedAuthorizedParty() {
        assertThatThrownBy(() -> converter.convert(jwt("identityhub-admin-login", "")))
                .isInstanceOf(BadJwtException.class);
        assertThatThrownBy(() -> converter.convert(jwt("ih-machine-not-a-uuid", "")))
                .isInstanceOf(BadJwtException.class);
    }

    private Jwt jwt(String authorizedParty, String scope) {
        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", "service-account",
                        "azp", authorizedParty,
                        "scope", scope,
                        "aud", List.of("identityhub-integration-api")));
    }
}
