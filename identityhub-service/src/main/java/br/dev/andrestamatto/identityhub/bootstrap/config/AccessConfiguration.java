package br.dev.andrestamatto.identityhub.bootstrap.config;

import br.dev.andrestamatto.identityhub.access.adapter.out.jdbc.JdbcMembershipGrantRepository;
import br.dev.andrestamatto.identityhub.access.application.GrantMembership;
import br.dev.andrestamatto.identityhub.access.application.GetMembershipOperation;
import br.dev.andrestamatto.identityhub.access.application.MembershipGrantRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionOperations;

@Configuration(proxyBeanMethods = false)
class AccessConfiguration {

    @Bean
    MembershipGrantRepository membershipGrantRepository(
            JdbcClient jdbcClient,
            TransactionOperations transactions) {
        return new JdbcMembershipGrantRepository(jdbcClient, transactions);
    }

    @Bean
    GrantMembership grantMembership(
            MembershipGrantRepository repository,
            Clock clock) {
        return new GrantMembership(repository, clock, UUID::randomUUID);
    }

    @Bean
    GetMembershipOperation getMembershipOperation(MembershipGrantRepository repository) {
        return new GetMembershipOperation(repository);
    }
}
