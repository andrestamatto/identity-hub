package br.dev.andrestamatto.identityhub.bootstrap.config;

import br.dev.andrestamatto.identityhub.clientapplication.adapter.out.jdbc.JdbcApplicationClientConfigurationRepository;
import br.dev.andrestamatto.identityhub.clientapplication.adapter.out.jdbc.JdbcClientApplicationRepository;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientConfigurationRepository;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientProjectionRepository;
import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationRepository;
import br.dev.andrestamatto.identityhub.clientapplication.application.ConfigureBffClient;
import br.dev.andrestamatto.identityhub.clientapplication.application.ConfigureMachineClient;
import br.dev.andrestamatto.identityhub.clientapplication.application.ConfigureProtectedApiClient;
import br.dev.andrestamatto.identityhub.clientapplication.application.ConfigureSpaClient;
import br.dev.andrestamatto.identityhub.clientapplication.application.ConfigureSelfRegistration;
import br.dev.andrestamatto.identityhub.clientapplication.application.GetApplicationClientConfiguration;
import br.dev.andrestamatto.identityhub.clientapplication.application.GetClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.application.RegisterClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.application.ReconcileApplicationClientProjection;
import br.dev.andrestamatto.identityhub.clientapplication.domain.BrowserTransportPolicy;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionOperations;

@Configuration(proxyBeanMethods = false)
class ClientApplicationConfiguration {

    @Bean
    ClientApplicationRepository clientApplicationRepository(JdbcClient jdbcClient) {
        return new JdbcClientApplicationRepository(jdbcClient);
    }

    @Bean
    JdbcApplicationClientConfigurationRepository applicationClientConfigurationRepository(
            JdbcClient jdbcClient,
            TransactionOperations transactions) {
        return new JdbcApplicationClientConfigurationRepository(jdbcClient, transactions);
    }

    @Bean
    ConfigureProtectedApiClient configureProtectedApiClient(
            ClientApplicationRepository applicationRepository,
            ApplicationClientConfigurationRepository clientRepository,
            Clock clock) {
        return new ConfigureProtectedApiClient(
                applicationRepository,
                clientRepository,
                clock,
                UUID::randomUUID);
    }

    @Bean
    ConfigureSpaClient configureSpaClient(
            ClientApplicationRepository applicationRepository,
            ApplicationClientConfigurationRepository clientRepository,
            BrowserTransportPolicy transportPolicy,
            Clock clock) {
        return new ConfigureSpaClient(
                applicationRepository,
                clientRepository,
                transportPolicy,
                clock,
                UUID::randomUUID);
    }

    @Bean
    ConfigureBffClient configureBffClient(
            ClientApplicationRepository applicationRepository,
            ApplicationClientConfigurationRepository clientRepository,
            BrowserTransportPolicy transportPolicy,
            Clock clock) {
        return new ConfigureBffClient(
                applicationRepository,
                clientRepository,
                transportPolicy,
                clock,
                UUID::randomUUID);
    }

    @Bean
    ConfigureMachineClient configureMachineClient(
            ClientApplicationRepository applicationRepository,
            ApplicationClientConfigurationRepository clientRepository,
            Clock clock) {
        return new ConfigureMachineClient(
                applicationRepository,
                clientRepository,
                clock,
                UUID::randomUUID);
    }

    @Bean
    BrowserTransportPolicy browserTransportPolicy(
            IdentityHubRuntimeProperties runtimeProperties) {
        return runtimeProperties.environment()
                        == IdentityHubRuntimeProperties.DeploymentEnvironment.PRODUCTION
                ? BrowserTransportPolicy.PRODUCTION
                : BrowserTransportPolicy.DEVELOPMENT;
    }

    @Bean
    GetApplicationClientConfiguration getApplicationClientConfiguration(
            ApplicationClientConfigurationRepository repository) {
        return new GetApplicationClientConfiguration(repository);
    }

    @Bean
    ReconcileApplicationClientProjection reconcileApplicationClientProjection(
            ApplicationClientConfigurationRepository configurationRepository,
            ApplicationClientProjectionRepository projectionRepository,
            Clock clock) {
        return new ReconcileApplicationClientProjection(
                configurationRepository,
                projectionRepository,
                clock);
    }

    @Bean
    RegisterClientApplication registerClientApplication(
            ClientApplicationRepository repository,
            Clock clock) {
        return new RegisterClientApplication(repository, clock);
    }

    @Bean
    GetClientApplication getClientApplication(ClientApplicationRepository repository) {
        return new GetClientApplication(repository);
    }

    @Bean
    ConfigureSelfRegistration configureSelfRegistration(
            ClientApplicationRepository repository) {
        return new ConfigureSelfRegistration(repository);
    }
}
