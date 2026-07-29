package br.dev.andrestamatto.identityhub.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneOffset;

import br.dev.andrestamatto.identityhub.bootstrap.config.IdentityHubRuntimeProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;

@SpringBootTest
class IdentityHubApplicationTest {

    @Autowired
    private Clock clock;

    @Autowired
    private IdentityHubRuntimeProperties runtimeProperties;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void startsWithDevelopmentEnvironmentAndUtcClock() {
        assertThat(runtimeProperties.environment())
                .isEqualTo(IdentityHubRuntimeProperties.DeploymentEnvironment.DEVELOPMENT);
        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void doesNotCreateDefaultUserCredentials() {
        assertThat(applicationContext.getBeansOfType(UserDetailsService.class)).isEmpty();
    }
}
