package br.dev.andrestamatto.identityhub.integration.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.HttpServerErrorException;

class IdentityHubSecurityAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    SecurityAutoConfiguration.class,
                    ServletWebSecurityAutoConfiguration.class,
                    OAuth2ResourceServerAutoConfiguration.class,
                    IdentityHubSecurityAutoConfiguration.class))
            .withPropertyValues(
                    "identityhub.security.issuer-uri=https://auth.example.test/realms/identityhub",
                    "identityhub.security.audience=catalog-api");

    @Test
    void createsACompleteSecureDefaultWhenTheConsumerProvidesNoChain() {
        contextRunner
                .withUserConfiguration(TestJwtDecoderConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(IdentityHubSecurityProperties.class);
                    assertThat(context).hasBean("identityHubJwtValidator");
                    assertThat(context).hasBean("identityHubJwtAuthenticationConverter");
                    assertThat(context).hasBean("identityHubSecurityFilterChain");
                    assertThat(context).hasSingleBean(SecurityFilterChain.class);
                });
    }

    @Test
    void backsOffWhenTheConsumerDefinesItsOwnSecurityFilterChain() {
        contextRunner
                .withUserConfiguration(TestJwtDecoderConfiguration.class, ConsumerSecurityConfiguration.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean("identityHubSecurityFilterChain");
                    assertThat(context).hasSingleBean(SecurityFilterChain.class);
                });
    }

    @Test
    void remainsInactiveWhenTheConsumerExplicitlyDisablesSecurity() {
        contextRunner
                .withPropertyValues("identityhub.security.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(IdentityHubSecurityProperties.class));
    }

    @Test
    void failsStartupWhenTheJwkSetCannotBeRetrieved() throws Exception {
        try (var issuer = TestJwtIssuer.startWithUnavailableJwks()) {
            new WebApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            SecurityAutoConfiguration.class,
                            ServletWebSecurityAutoConfiguration.class,
                            OAuth2ResourceServerAutoConfiguration.class,
                            IdentityHubSecurityAutoConfiguration.class))
                    .withPropertyValues(
                            "identityhub.security.issuer-uri=" + issuer.issuer(),
                            "identityhub.security.audience=catalog-api",
                            "identityhub.security.allow-http-for-loopback=true")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(rootCauseOf(context.getStartupFailure()))
                                .isInstanceOf(HttpServerErrorException.ServiceUnavailable.class);
                    });
        }
    }

    private static Throwable rootCauseOf(Throwable failure) {
        var rootCause = failure;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    @Configuration(proxyBeanMethods = false)
    static class TestJwtDecoderConfiguration {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                var issuedAt = Instant.parse("2026-08-03T12:00:00Z");
                return new Jwt(
                        token,
                        issuedAt,
                        issuedAt.plusSeconds(300),
                        Map.of("alg", "RS256"),
                        Map.of("sub", "account-123"));
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ConsumerSecurityConfiguration {

        @Bean
        SecurityFilterChain consumerSecurityFilterChain(HttpSecurity http) throws Exception {
            return http.authorizeHttpRequests(authorize -> authorize.anyRequest().denyAll()).build();
        }
    }
}
