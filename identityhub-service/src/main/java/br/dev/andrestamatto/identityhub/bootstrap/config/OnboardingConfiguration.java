package br.dev.andrestamatto.identityhub.bootstrap.config;

import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientConfigurationRepository;
import br.dev.andrestamatto.identityhub.clientapplication.application.ResolveOnboardingOrigin;
import br.dev.andrestamatto.identityhub.identity.adapter.out.crypto.SecureRandomOnboardingSessionIdGenerator;
import br.dev.andrestamatto.identityhub.identity.adapter.out.crypto.SecureRandomOnboardingProofTokenGenerator;
import br.dev.andrestamatto.identityhub.identity.adapter.out.jdbc.JdbcOnboardingSessionRepository;
import br.dev.andrestamatto.identityhub.identity.adapter.out.jdbc.SpringOnboardingProofTransaction;
import br.dev.andrestamatto.identityhub.identity.application.BeginOnboardingSession;
import br.dev.andrestamatto.identityhub.identity.application.IssueOnboardingIdentityProof;
import br.dev.andrestamatto.identityhub.identity.application.OnboardingOriginResolver;
import br.dev.andrestamatto.identityhub.identity.application.OnboardingSessionRepository;
import java.security.SecureRandom;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionOperations;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "identityhub.onboarding.enabled", havingValue = "true")
class OnboardingConfiguration {

    @Bean
    OnboardingSessionRepository onboardingSessionRepository(JdbcClient jdbcClient) {
        return new JdbcOnboardingSessionRepository(jdbcClient);
    }

    @Bean
    ResolveOnboardingOrigin resolveOnboardingOrigin(
            ApplicationClientConfigurationRepository repository) {
        return new ResolveOnboardingOrigin(repository);
    }

    @Bean
    OnboardingOriginResolver onboardingOriginResolver(ResolveOnboardingOrigin resolveOrigin) {
        return (machineClientId, browserClientId, redirectUri) -> resolveOrigin.execute(
                machineClientId, browserClientId, redirectUri).applicationId();
    }

    @Bean
    BeginOnboardingSession beginOnboardingSession(
            OnboardingOriginResolver originResolver,
            OnboardingSessionRepository repository,
            Clock clock) {
        return new BeginOnboardingSession(
                originResolver,
                repository,
                clock,
                new SecureRandomOnboardingSessionIdGenerator(new SecureRandom()));
    }

    @Bean
    IssueOnboardingIdentityProof issueOnboardingIdentityProof(
            OnboardingSessionRepository repository,
            TransactionOperations transactions,
            Clock clock) {
        return new IssueOnboardingIdentityProof(
                repository,
                new SpringOnboardingProofTransaction(transactions),
                clock,
                new SecureRandomOnboardingProofTokenGenerator(new SecureRandom()));
    }
}
