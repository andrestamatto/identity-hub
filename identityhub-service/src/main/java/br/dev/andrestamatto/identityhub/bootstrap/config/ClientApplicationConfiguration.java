package br.dev.andrestamatto.identityhub.bootstrap.config;

import br.dev.andrestamatto.identityhub.clientapplication.adapter.out.jdbc.JdbcClientApplicationRepository;
import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationRepository;
import br.dev.andrestamatto.identityhub.clientapplication.application.GetClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.application.RegisterClientApplication;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration(proxyBeanMethods = false)
class ClientApplicationConfiguration {

    @Bean
    ClientApplicationRepository clientApplicationRepository(JdbcClient jdbcClient) {
        return new JdbcClientApplicationRepository(jdbcClient);
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
}
