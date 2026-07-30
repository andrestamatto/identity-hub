package br.dev.andrestamatto.identityhub.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class AdminJwtAuthenticationConverterTest {

    @Test
    void mapsPlatformRolesAndTotpEvidenceToAuthorities() {
        var authentication = new AdminJwtAuthenticationConverter().convert(jwt(
                List.of("PLATFORM_ADMIN"),
                List.of("pwd", "totp")));

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_PLATFORM_ADMIN", "MFA_TOTP");
    }

    @Test
    void doesNotInventTotpEvidenceWhenClaimIsMissing() {
        var authentication = new AdminJwtAuthenticationConverter().convert(jwt(
                List.of("PLATFORM_AUDITOR"),
                List.of("pwd")));

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_PLATFORM_AUDITOR");
    }

    private Jwt jwt(List<String> roles, List<String> authenticationMethods) {
        var issuedAt = Instant.parse("2026-07-29T12:00:00Z");
        return new Jwt(
                "token-value",
                issuedAt,
                issuedAt.plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", "operator-id",
                        "realm_access", Map.of("roles", roles),
                        "amr", authenticationMethods));
    }
}
