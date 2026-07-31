package br.dev.andrestamatto.identityhub.bootstrap.config;

import br.dev.andrestamatto.identityhub.clientapplication.adapter.out.keycloak.KeycloakApplicationClientProjector;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientProjectionRepository;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientProjector;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientConfigurationRepository;
import br.dev.andrestamatto.identityhub.clientapplication.application.ProcessApplicationClientProjection;
import br.dev.andrestamatto.identityhub.clientapplication.application.RotateBffClientSecret;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.http.HttpClient;
import java.time.Clock;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(
        name = "identityhub.keycloak.management.enabled",
        havingValue = "true")
class ApplicationClientProjectionConfiguration {

    @Bean
    KeycloakApplicationClientProjector applicationClientProjector(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            KeycloakManagementProperties properties) {
        return new KeycloakApplicationClientProjector(
                httpClient,
                objectMapper,
                properties.baseUri(),
                properties.realm(),
                properties.clientId(),
                properties.clientSecret());
    }

    @Bean
    RotateBffClientSecret rotateBffClientSecret(
            ApplicationClientConfigurationRepository repository,
            KeycloakApplicationClientProjector projector) {
        return new RotateBffClientSecret(repository, projector);
    }

    @Bean
    ProcessApplicationClientProjection processApplicationClientProjection(
            ApplicationClientProjectionRepository repository,
            ApplicationClientProjector projector,
            Clock clock,
            KeycloakManagementProperties properties) {
        return new ProcessApplicationClientProjection(
                repository,
                projector,
                clock,
                properties.leaseDuration(),
                properties.initialRetryDelay(),
                properties.maxAttempts());
    }

    @Bean
    ApplicationClientProjectionScheduler applicationClientProjectionScheduler(
            ProcessApplicationClientProjection processor,
            MeterRegistry registry) {
        return new ApplicationClientProjectionScheduler(
                processor,
                registry,
                UUID.randomUUID());
    }
}
