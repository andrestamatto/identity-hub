package br.dev.andrestamatto.identityhub.bootstrap.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class IdentityHubRuntimePropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(RuntimePropertiesConfiguration.class);

    @Test
    void rejectsUnknownRuntimeEnvironment() {
        contextRunner
                .withPropertyValues("identityhub.runtime.environment=unknown")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("identityhub.runtime");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(IdentityHubRuntimeProperties.class)
    static class RuntimePropertiesConfiguration {
    }
}
