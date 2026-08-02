package br.dev.andrestamatto.identityhub.bootstrap.config;

import br.dev.andrestamatto.identityhub.clientapplication.application.GetClientApplication;
import br.dev.andrestamatto.identityhub.communication.application.RequestEmailVerificationEmail;
import br.dev.andrestamatto.identityhub.communication.application.RequestPasswordRecoveryEmail;
import br.dev.andrestamatto.identityhub.identity.adapter.out.communication.CommunicationRecoveryEmailRequester;
import br.dev.andrestamatto.identityhub.identity.adapter.out.communication.CommunicationVerificationEmailRequester;
import br.dev.andrestamatto.identityhub.identity.adapter.out.crypto.SecureRandomEmailVerificationSecretGenerator;
import br.dev.andrestamatto.identityhub.identity.adapter.out.crypto.SecureRandomPasswordRecoverySecretGenerator;
import br.dev.andrestamatto.identityhub.identity.adapter.out.jdbc.JdbcEmailVerificationChallengeRepository;
import br.dev.andrestamatto.identityhub.identity.adapter.out.jdbc.JdbcPasswordRecoveryChallengeRepository;
import br.dev.andrestamatto.identityhub.identity.adapter.out.jdbc.SpringVerificationTransaction;
import br.dev.andrestamatto.identityhub.identity.adapter.out.clientapplication.ClientApplicationSelfRegistrationPolicyResolver;
import br.dev.andrestamatto.identityhub.identity.adapter.out.keycloak.KeycloakLocalIdentityRegistrar;
import br.dev.andrestamatto.identityhub.identity.application.RegisterPendingLocalIdentity;
import br.dev.andrestamatto.identityhub.identity.application.ConfirmEmailVerification;
import br.dev.andrestamatto.identityhub.identity.application.BeginLocalRegistration;
import br.dev.andrestamatto.identityhub.identity.application.RequestEmailVerification;
import br.dev.andrestamatto.identityhub.identity.application.RequestPasswordRecovery;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionOperations;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        name = "identityhub.keycloak.identity-management.enabled",
        havingValue = "true")
class IdentityManagementConfiguration {

    @Bean
    ClientApplicationSelfRegistrationPolicyResolver selfRegistrationPolicyResolver(
            GetClientApplication getClientApplication) {
        return new ClientApplicationSelfRegistrationPolicyResolver(getClientApplication);
    }

    @Bean
    KeycloakLocalIdentityRegistrar localIdentityRegistrar(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            IdentityManagementProperties properties) {
        return new KeycloakLocalIdentityRegistrar(
                httpClient,
                objectMapper,
                properties.baseUri(),
                properties.realm(),
                properties.clientId(),
                properties.clientSecret());
    }

    @Bean
    RegisterPendingLocalIdentity registerPendingLocalIdentity(
            ClientApplicationSelfRegistrationPolicyResolver policyResolver,
            KeycloakLocalIdentityRegistrar registrar) {
        return new RegisterPendingLocalIdentity(policyResolver, registrar);
    }

    @Bean
    JdbcEmailVerificationChallengeRepository emailVerificationChallengeRepository(
            JdbcClient jdbcClient) {
        return new JdbcEmailVerificationChallengeRepository(jdbcClient);
    }

    @Bean
    SpringVerificationTransaction verificationTransaction(
            TransactionOperations transactions) {
        return new SpringVerificationTransaction(transactions);
    }

    @Bean
    CommunicationVerificationEmailRequester verificationEmailRequester(
            RequestEmailVerificationEmail requestEmail) {
        return new CommunicationVerificationEmailRequester(requestEmail);
    }

    @Bean
    RequestEmailVerification requestEmailVerification(
            JdbcEmailVerificationChallengeRepository repository,
            CommunicationVerificationEmailRequester emailRequester,
            SpringVerificationTransaction transaction,
            Clock clock,
            IdentityManagementProperties properties) {
        return new RequestEmailVerification(
                repository,
                emailRequester,
                transaction,
                new SecureRandomEmailVerificationSecretGenerator(new SecureRandom()),
                clock,
                UUID::randomUUID,
                properties.publicBaseUri());
    }

    @Bean
    ConfirmEmailVerification confirmEmailVerification(
            JdbcEmailVerificationChallengeRepository repository,
            KeycloakLocalIdentityRegistrar registrar,
            SpringVerificationTransaction transaction,
            Clock clock) {
        return new ConfirmEmailVerification(repository, registrar, transaction, clock);
    }

    @Bean
    JdbcPasswordRecoveryChallengeRepository passwordRecoveryChallengeRepository(
            JdbcClient jdbcClient) {
        return new JdbcPasswordRecoveryChallengeRepository(jdbcClient);
    }

    @Bean
    CommunicationRecoveryEmailRequester recoveryEmailRequester(
            RequestPasswordRecoveryEmail requestEmail) {
        return new CommunicationRecoveryEmailRequester(requestEmail);
    }

    @Bean
    RequestPasswordRecovery requestPasswordRecovery(
            KeycloakLocalIdentityRegistrar identityFinder,
            JdbcPasswordRecoveryChallengeRepository repository,
            CommunicationRecoveryEmailRequester emailRequester,
            SpringVerificationTransaction transaction,
            Clock clock,
            IdentityManagementProperties properties) {
        return new RequestPasswordRecovery(
                identityFinder,
                repository,
                emailRequester,
                transaction,
                new SecureRandomPasswordRecoverySecretGenerator(new SecureRandom()),
                clock,
                UUID::randomUUID,
                properties.publicBaseUri());
    }

    @Bean
    BeginLocalRegistration beginLocalRegistration(
            RegisterPendingLocalIdentity registerIdentity,
            RequestEmailVerification requestVerification) {
        return new BeginLocalRegistration(registerIdentity, requestVerification);
    }
}
