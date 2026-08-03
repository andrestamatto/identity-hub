package br.dev.andrestamatto.identityhub.bootstrap.config;

import br.dev.andrestamatto.identityhub.access.adapter.out.jdbc.JdbcMembershipProjectionRepository;
import br.dev.andrestamatto.identityhub.access.adapter.out.keycloak.KeycloakMembershipProjector;
import br.dev.andrestamatto.identityhub.access.adapter.out.keycloak.KeycloakMembershipTokenProjector;
import br.dev.andrestamatto.identityhub.access.application.MembershipProjector;
import br.dev.andrestamatto.identityhub.access.application.ProcessMembershipProjection;
import br.dev.andrestamatto.identityhub.clientapplication.adapter.out.jdbc.JdbcApplicationTokenClientResolver;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.http.HttpClient;
import java.time.Clock;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.support.TransactionOperations;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(
        name = {
            "identityhub.keycloak.identity-management.enabled",
            "identityhub.keycloak.management.enabled"
        },
        havingValue = "true")
class MembershipProjectionConfiguration {

    @Bean
    JdbcMembershipProjectionRepository membershipProjectionRepository(
            JdbcClient jdbcClient,
            TransactionOperations transactions) {
        return new JdbcMembershipProjectionRepository(jdbcClient, transactions);
    }

    @Bean
    JdbcApplicationTokenClientResolver applicationTokenClientResolver(JdbcClient jdbcClient) {
        return new JdbcApplicationTokenClientResolver(jdbcClient);
    }

    @Bean
    MembershipProjector membershipProjector(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            IdentityManagementProperties identityProperties,
            KeycloakManagementProperties managementProperties,
            JdbcApplicationTokenClientResolver clientResolver) {
        var markerProjector = new KeycloakMembershipProjector(
                httpClient,
                objectMapper,
                identityProperties.baseUri(),
                identityProperties.realm(),
                identityProperties.clientId(),
                identityProperties.clientSecret());
        var tokenProjector = new KeycloakMembershipTokenProjector(
                httpClient,
                objectMapper,
                managementProperties.baseUri(),
                managementProperties.realm(),
                managementProperties.clientId(),
                managementProperties.clientSecret(),
                clientResolver);
        return membership -> {
            var roles = tokenProjector.project(membership);
            markerProjector.project(membership, roles);
        };
    }

    @Bean
    ProcessMembershipProjection processMembershipProjection(
            JdbcMembershipProjectionRepository repository,
            MembershipProjector projector,
            Clock clock,
            KeycloakManagementProperties managementProperties) {
        return new ProcessMembershipProjection(
                repository,
                projector,
                clock,
                managementProperties.leaseDuration(),
                managementProperties.initialRetryDelay(),
                managementProperties.maxAttempts());
    }

    @Bean
    MembershipProjectionScheduler membershipProjectionScheduler(
            ProcessMembershipProjection processor,
            MeterRegistry registry) {
        return new MembershipProjectionScheduler(processor, registry, UUID.randomUUID());
    }
}
