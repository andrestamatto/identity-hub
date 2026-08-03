package br.dev.andrestamatto.identityhub.integration.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class IdentityHubJwtAuthenticationConverterTest {

    private final IdentityHubJwtAuthenticationConverter converter = new IdentityHubJwtAuthenticationConverter(
            new IdentityHubSecurityProperties.Authorities("ROLE_", "SCOPE_"));

    @Test
    void mapsOnlyDocumentedScopeAndRoleClaims() {
        var authentication = converter.convert(jwt());

        assertThat(authentication.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactlyInAnyOrder(
                        "SCOPE_catalog:read",
                        "SCOPE_catalog:write",
                        "ROLE_catalog-reader")
                .doesNotContain("ROLE_PLATFORM_ADMIN", "ROLE_private-client-role", "SCOPE_private");
    }

    @Test
    void appliesConfiguredAuthorityPrefixes() {
        var customConverter = new IdentityHubJwtAuthenticationConverter(
                new IdentityHubSecurityProperties.Authorities("APP_", "PERM_"));

        assertThat(customConverter.convert(jwt()).getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .contains("PERM_catalog:read", "PERM_catalog:write", "APP_catalog-reader");
    }

    private Jwt jwt() {
        var issuedAt = Instant.parse("2026-08-03T12:00:00Z");
        return new Jwt(
                "synthetic-token",
                issuedAt,
                issuedAt.plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", "account-123",
                        "scope", "catalog:read catalog:write",
                        "roles", List.of("catalog-reader"),
                        "realm_access", Map.of("roles", List.of("PLATFORM_ADMIN")),
                        "resource_access", Map.of("private-client", Map.of("roles", List.of("private-client-role"))),
                        "groups", List.of("private"),
                        "email", "andrew@example.test",
                        "phone_number", "+5511999999999"));
    }
}
