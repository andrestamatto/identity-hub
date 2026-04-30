package br.dev.andrestamatto.identityhub.infrastructure.config;

import br.dev.andrestamatto.identityhub.application.exception.IdentitySourceUnavailableException;
import br.dev.andrestamatto.identityhub.application.ports.LoadExternalIdentity;
import br.dev.andrestamatto.identityhub.application.ports.LoadSocialIdentity;
import br.dev.andrestamatto.identityhub.application.ports.ResolveSocialUser;
import br.dev.andrestamatto.identityhub.application.usecase.PasswordLogin;
import br.dev.andrestamatto.identityhub.application.usecase.PasswordLoginUseCase;
import br.dev.andrestamatto.identityhub.application.usecase.SocialLogin;
import br.dev.andrestamatto.identityhub.application.usecase.SocialLoginUseCase;
import br.dev.andrestamatto.identityhub.domain.service.PasswordEncoder;
import br.dev.andrestamatto.identityhub.domain.service.PasswordLoginAuthenticator;
import br.dev.andrestamatto.identityhub.domain.service.PasswordLoginAuthenticatorService;
import br.dev.andrestamatto.identityhub.infrastructure.security.TokenService;
import br.dev.andrestamatto.identityhub.infrastructure.security.jwt.JwtService;
import br.dev.andrestamatto.identityhub.infrastructure.security.password.BCryptPasswordEncoderAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(IdentityHubSocialLoginProperties.class)
public class AuthenticatableConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticatableConfig.class);

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoderAdapter();
    }

    @Bean
    @ConditionalOnBean(LoadExternalIdentity.class)
    public PasswordLoginAuthenticator passwordLoginAuthenticator(PasswordEncoder passwordEncoder, LoadExternalIdentity loadExternalIdentity) {
        return new PasswordLoginAuthenticatorService(passwordEncoder, loadExternalIdentity);
    }

    @Bean
    public TokenService tokenService(JwtService jwtService) {
        return jwtService;
    }

    @Bean
    @ConditionalOnBean(PasswordLoginAuthenticator.class)
    public PasswordLoginUseCase login(PasswordLoginAuthenticator passwordLoginAuthenticator, TokenService tokenService) {
        return new PasswordLoginUseCase(passwordLoginAuthenticator, tokenService);
    }

    @Bean
    @ConditionalOnMissingBean(PasswordLogin.class)
    public PasswordLogin unavailableLogin() {
        return (requestIdentity, requestPassword) -> {
            throw new IdentitySourceUnavailableException();
        };
    }

    @Bean
    @ConditionalOnBean({LoadSocialIdentity.class, ResolveSocialUser.class})
    @ConditionalOnProperty(prefix = "identity-hub.social-login", name = "enabled", havingValue = "true")
    public SocialLogin socialLoginUseCase(
            LoadSocialIdentity loadSocialIdentity,
            ResolveSocialUser resolveSocialUser,
            TokenService tokenService,
            IdentityHubSocialLoginProperties socialLoginProperties
    ) {
        return new SocialLoginUseCase(loadSocialIdentity, resolveSocialUser, tokenService, socialLoginProperties);
    }

    @Bean
    @ConditionalOnMissingBean(SocialLogin.class)
    public SocialLogin unavailableSocialLoginUseCase(
            IdentityHubSocialLoginProperties socialLoginProperties
    ) {
        return (provider, authorizationCode, redirectUri) -> {
            if (!socialLoginProperties.enabled()) {
                throw new IdentitySourceUnavailableException("Social login is disabled in configuration.");
            }
            throw new IdentitySourceUnavailableException(
                    "Social login is enabled, but LoadSocialIdentity and/or ResolveSocialUser is not configured."
            );
        };
    }

    @Bean
    @ConditionalOnMissingBean(LoadExternalIdentity.class)
    public ApplicationRunner missingIdentitySourceWarning() {
        return args -> LOGGER.warn(
                "No LoadExternalIdentity bean found. Endpoint /auth/login will return 503 until an identity source is configured."
        );
    }

    @Bean
    @ConditionalOnProperty(prefix = "identity-hub.social-login", name = "enabled", havingValue = "false")
    public ApplicationRunner socialLoginDisabledWarning() {
        return args -> LOGGER.warn(
                "Social login is disabled by configuration (identity-hub.social-login.enabled=false)."
        );
    }

    @Bean
    @ConditionalOnProperty(prefix = "identity-hub.social-login", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean({LoadSocialIdentity.class, ResolveSocialUser.class})
    public ApplicationRunner missingSocialIdentitySourceWarning() {
        return args -> LOGGER.warn(
                "Social login is enabled, but LoadSocialIdentity and/or ResolveSocialUser beans are missing. OAuth callback endpoints will return 503."
        );
    }
}
