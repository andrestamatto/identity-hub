package br.dev.andrestamatto.identityhub.infrastructure.config;

import br.dev.andrestamatto.identityhub.application.ports.LoadExternalIdentity;
import br.dev.andrestamatto.identityhub.application.ports.LoadSocialIdentity;
import br.dev.andrestamatto.identityhub.application.ports.ResolveSocialUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(IdentityHubSocialLoginProperties.class)
public class AuthWarningsConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthWarningsConfiguration.class);

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
