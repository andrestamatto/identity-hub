package br.dev.andrestamatto.identityhub.infrastructure.config;

import br.dev.andrestamatto.identityhub.application.exception.IdentitySourceUnavailableException;
import br.dev.andrestamatto.identityhub.application.ports.LoadExternalIdentityPort;
import br.dev.andrestamatto.identityhub.application.ports.TokenServicePort;
import br.dev.andrestamatto.identityhub.application.usecase.PasswordLoginUseCase;
import br.dev.andrestamatto.identityhub.application.usecase.port.in.PasswordLoginUseCasePort;
import br.dev.andrestamatto.identityhub.domain.service.PasswordEncoder;
import br.dev.andrestamatto.identityhub.domain.service.PasswordLoginAuthenticator;
import br.dev.andrestamatto.identityhub.domain.service.PasswordLoginAuthenticatorService;
import br.dev.andrestamatto.identityhub.infrastructure.security.password.BCryptPasswordEncoderAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PasswordAuthConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoderAdapter();
    }

    @Bean
    @ConditionalOnBean(LoadExternalIdentityPort.class)
    public PasswordLoginAuthenticator passwordLoginAuthenticator(PasswordEncoder passwordEncoder, LoadExternalIdentityPort loadExternalIdentityPort) {
        return new PasswordLoginAuthenticatorService(passwordEncoder, loadExternalIdentityPort);
    }

    @Bean
    @ConditionalOnBean(PasswordLoginAuthenticator.class)
    public PasswordLoginUseCase passwordLoginUseCase(PasswordLoginAuthenticator passwordLoginAuthenticator, TokenServicePort tokenServicePort) {
        return new PasswordLoginUseCase(passwordLoginAuthenticator, tokenServicePort);
    }

    @Bean
    @ConditionalOnMissingBean(PasswordLoginUseCasePort.class)
    public PasswordLoginUseCasePort unavailablePasswordLoginUseCase() {
        return (requestIdentity, requestPassword) -> {
            throw new IdentitySourceUnavailableException();
        };
    }
}
