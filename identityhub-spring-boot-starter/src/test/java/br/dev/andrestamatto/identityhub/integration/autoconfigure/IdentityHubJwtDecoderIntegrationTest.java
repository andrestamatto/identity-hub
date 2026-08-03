package br.dev.andrestamatto.identityhub.integration.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class IdentityHubJwtDecoderIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-03T12:00:00Z");
    private TestJwtIssuer jwtIssuer;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() throws Exception {
        jwtIssuer = TestJwtIssuer.start();
        var properties = new IdentityHubSecurityProperties(
                true,
                URI.create(jwtIssuer.issuer()),
                "catalog-api",
                Duration.ofSeconds(60),
                true,
                null);
        var configuration = new IdentityHubSecurityAutoConfiguration();
        OAuth2TokenValidator<Jwt> validator = configuration.identityHubJwtValidator(
                properties,
                Clock.fixed(NOW, java.time.ZoneOffset.UTC));
        decoder = configuration.identityHubJwtDecoder(
                properties,
                validator,
                configuration.identityHubJwtRestOperations());
    }

    @AfterEach
    void tearDown() {
        jwtIssuer.close();
    }

    @Test
    void acceptsAValidRs256TokenForTheConfiguredIssuerAndAudience() throws Exception {
        var token = jwtIssuer.issueAccessToken(
                jwtIssuer.issuer(),
                List.of("catalog-api"),
                NOW,
                NOW.plusSeconds(300),
                "catalog:read",
                List.of("catalog-reader"));

        assertThat(decoder.decode(token).getSubject()).isEqualTo("account-123");
    }

    @Test
    void rejectsWrongAudienceIssuerExpiryFutureIssuedAtAndUnknownKey() throws Exception {
        assertRejected(jwtIssuer.issueAccessToken(
                jwtIssuer.issuer(),
                List.of("another-api"),
                NOW,
                NOW.plusSeconds(300),
                "catalog:read",
                List.of()));
        assertRejected(jwtIssuer.issueAccessToken(
                "http://127.0.0.1:9999",
                List.of("catalog-api"),
                NOW,
                NOW.plusSeconds(300),
                "catalog:read",
                List.of()));
        assertRejected(jwtIssuer.issueAccessToken(
                jwtIssuer.issuer(),
                List.of("catalog-api"),
                NOW.minusSeconds(300),
                NOW.minusSeconds(61),
                "catalog:read",
                List.of()));
        assertRejected(jwtIssuer.issueAccessToken(
                jwtIssuer.issuer(),
                List.of("catalog-api"),
                NOW.plusSeconds(61),
                NOW.plusSeconds(300),
                "catalog:read",
                List.of()));
        assertRejected(jwtIssuer.issueAccessTokenWithUnknownKey(
                List.of("catalog-api"),
                NOW,
                NOW.plusSeconds(300)));
        assertRejected(jwtIssuer.issueHs256Token(
                List.of("catalog-api"),
                NOW,
                NOW.plusSeconds(300)));
    }

    private void assertRejected(String token) {
        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }
}
