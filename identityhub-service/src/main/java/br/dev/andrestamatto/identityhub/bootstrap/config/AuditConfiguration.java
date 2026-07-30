package br.dev.andrestamatto.identityhub.bootstrap.config;

import br.dev.andrestamatto.identityhub.audit.adapter.out.jdbc.JdbcAdministrativeAccessEventRepository;
import br.dev.andrestamatto.identityhub.audit.application.AdministrativeAccessAudit;
import br.dev.andrestamatto.identityhub.audit.application.AdministrativeAccessEventRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.util.IdGenerator;

@Configuration(proxyBeanMethods = false)
class AuditConfiguration {

    @Bean
    AdministrativeAccessEventRepository administrativeAccessEventRepository(
            JdbcClient jdbcClient) {
        return new JdbcAdministrativeAccessEventRepository(jdbcClient);
    }

    @Bean
    AdministrativeAccessAudit administrativeAccessAudit(
            AdministrativeAccessEventRepository repository,
            Clock clock,
            IdGenerator identifierGenerator) {
        return new AdministrativeAccessAudit(repository, clock, identifierGenerator::generateId);
    }
}
