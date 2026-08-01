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

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude="
            + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
            + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
    "management.endpoint.health.group.readiness.include=readinessState,keycloak",
    "identityhub.security.admin.issuer-uri=https://auth.dev.example/realms/identityhub",
    "identityhub.security.admin.jwk-set-uri=https://auth.dev.example/realms/identityhub/certs",
    "identityhub.security.admin.audience=identityhub-admin-api"
})
class IdentityHubApplicationTest {

    @Autowired
    private Clock clock;

    @Autowired
    private IdentityHubRuntimeProperties runtimeProperties;

    @Autowired
    private ApplicationContext applicationContext;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private br.dev.andrestamatto.identityhub.audit.application.AdministrativeAccessEventRepository
            administrativeAccessEventRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationRepository
            clientApplicationRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private br.dev.andrestamatto.identityhub.clientapplication.adapter.out.jdbc
                    .JdbcApplicationClientConfigurationRepository
            applicationClientConfigurationRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private br.dev.andrestamatto.identityhub.communication.application.EmailDeliveryRepository
            emailDeliveryRepository;

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
