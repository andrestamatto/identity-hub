package br.dev.andrestamatto.identityhub.infrastructure.config;

import br.dev.andrestamatto.identityhub.application.ports.LoadExternalIdentity;
import br.dev.andrestamatto.identityhub.application.exception.IdentitySourceUnavailableException;
import br.dev.andrestamatto.identityhub.application.usecase.Authenticatable;
import br.dev.andrestamatto.identityhub.application.usecase.Login;
import br.dev.andrestamatto.identityhub.domain.service.AuthProvider;
import br.dev.andrestamatto.identityhub.domain.service.LoginProvider;
import br.dev.andrestamatto.identityhub.domain.service.PasswordEncoder;
import br.dev.andrestamatto.identityhub.infrastructure.security.TokenService;
import br.dev.andrestamatto.identityhub.infrastructure.security.jwt.JwtService;
import br.dev.andrestamatto.identityhub.infrastructure.security.password.BCryptPasswordEncoderAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthenticatableConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticatableConfig.class);

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoderAdapter();
    }

    @Bean
    @ConditionalOnBean(LoadExternalIdentity.class)
    public AuthProvider loginProvider(PasswordEncoder passwordEncoder, LoadExternalIdentity loadExternalIdentity) {
        return new LoginProvider(passwordEncoder, loadExternalIdentity);
    }

    @Bean
    public TokenService tokenService(JwtService jwtService) {
        return jwtService;
    }

    @Bean
    @ConditionalOnBean(AuthProvider.class)
    public Authenticatable login(AuthProvider authProvider, TokenService tokenService) {
        return new Login(authProvider, tokenService);
    }

    @Bean
    @ConditionalOnMissingBean(Authenticatable.class)
    public Authenticatable unavailableLogin() {
        return (requestIdentity, requestPassword) -> {
            throw new IdentitySourceUnavailableException();
        };
    }

    @Bean
    @ConditionalOnMissingBean(LoadExternalIdentity.class)
    public ApplicationRunner missingIdentitySourceWarning() {
        return args -> LOGGER.warn(
                "No LoadExternalIdentity bean found. Endpoint /auth/login will return 503 until an identity source is configured."
        );
    }
}
