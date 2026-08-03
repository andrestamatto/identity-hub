package br.dev.andrestamatto.identityhub.integration.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.junit.jupiter.api.Test;

class IdentityHubSecurityPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);


    @Test
    void acceptsHttpsIssuerAndAppliesSafeDefaults() {
        var properties = new IdentityHubSecurityProperties(
                true,
                URI.create("https://auth.example.test/realms/identityhub"),
                "catalog-api",
                null,
                false,
                null);

        assertThat(properties.clockSkew()).isEqualTo(Duration.ofSeconds(60));
        assertThat(properties.authorities().rolePrefix()).isEqualTo("ROLE_");
        assertThat(properties.authorities().scopePrefix()).isEqualTo("SCOPE_");
    }

    @Test
    void rejectsMissingIssuerWhenSecurityIsEnabled() {
        assertThatIllegalArgumentException().isThrownBy(() -> new IdentityHubSecurityProperties(
                true,
                null,
                "catalog-api",
                Duration.ofSeconds(60),
                false,
                null));
    }

    @Test
    void rejectsHttpIssuerUnlessItIsAnExplicitLoopbackException() {
        assertThatIllegalArgumentException().isThrownBy(() -> new IdentityHubSecurityProperties(
                true,
                URI.create("http://auth.example.test/realms/identityhub"),
                "catalog-api",
                Duration.ofSeconds(60),
                true,
                null));

        var localProperties = new IdentityHubSecurityProperties(
                true,
                URI.create("http://127.0.0.1:8080/realms/identityhub"),
                "catalog-api",
                Duration.ofSeconds(60),
                true,
                null);

        assertThat(localProperties.issuerUri().getHost()).isEqualTo("127.0.0.1");
    }

    @Test
    void rejectsUnsafeAudiencePrefixesAndClockSkew() {
        var issuer = URI.create("https://auth.example.test/realms/identityhub");

        assertThatIllegalArgumentException().isThrownBy(() -> new IdentityHubSecurityProperties(
                true,
                issuer,
                "catalog-*",
                Duration.ofSeconds(60),
                false,
                null));
        assertThatIllegalArgumentException().isThrownBy(() -> new IdentityHubSecurityProperties(
                true,
                issuer,
                "catalog-api",
                Duration.ofSeconds(-1),
                false,
                null));
        assertThatIllegalArgumentException().isThrownBy(() -> new IdentityHubSecurityProperties(
                true,
                issuer,
                "catalog-api",
                Duration.ofSeconds(60),
                false,
                new IdentityHubSecurityProperties.Authorities("", "SCOPE_")));
    }

    @Test
    void permitsAnExplicitlyDisabledIntegrationWithoutIssuerOrAudience() {
        var properties = new IdentityHubSecurityProperties(false, null, null, null, false, null);

        assertThat(properties.enabled()).isFalse();
    }

    @Test
    void bindsEnabledAndSafeDefaultsFromSpringConfiguration() {
        contextRunner
                .withPropertyValues(
                        "identityhub.security.issuer-uri=https://auth.example.test/realms/identityhub",
                        "identityhub.security.audience=catalog-api")
                .run(context -> {
                    var properties = context.getBean(IdentityHubSecurityProperties.class);

                    assertThat(properties.enabled()).isTrue();
                    assertThat(properties.clockSkew()).isEqualTo(Duration.ofSeconds(60));
                    assertThat(properties.authorities().rolePrefix()).isEqualTo("ROLE_");
                    assertThat(properties.authorities().scopePrefix()).isEqualTo("SCOPE_");
                });
    }

    @Test
    void failsSpringStartupWhenTheDefaultEnabledModeLacksIssuer() {
        contextRunner
                .withPropertyValues("identityhub.security.audience=catalog-api")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(IdentityHubSecurityProperties.class)
    static class PropertiesConfiguration {
    }
}
