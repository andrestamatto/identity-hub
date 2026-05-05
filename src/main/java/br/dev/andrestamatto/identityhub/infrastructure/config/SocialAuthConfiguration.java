package br.dev.andrestamatto.identityhub.infrastructure.config;

import br.dev.andrestamatto.identityhub.application.exception.IdentitySourceUnavailableException;
import br.dev.andrestamatto.identityhub.application.ports.LoadSocialIdentity;
import br.dev.andrestamatto.identityhub.application.ports.ResolveSocialUser;
import br.dev.andrestamatto.identityhub.application.ports.SocialProviderPolicyPort;
import br.dev.andrestamatto.identityhub.application.ports.TokenServicePort;
import br.dev.andrestamatto.identityhub.application.usecase.SocialLogin;
import br.dev.andrestamatto.identityhub.application.usecase.SocialLoginUseCase;
import br.dev.andrestamatto.identityhub.infrastructure.social.PropertiesSocialProviderPolicyAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(IdentityHubSocialLoginProperties.class)
public class SocialAuthConfiguration {

    @Bean
    public SocialProviderPolicyPort socialProviderPolicyPort(IdentityHubSocialLoginProperties socialLoginProperties) {
        return new PropertiesSocialProviderPolicyAdapter(socialLoginProperties);
    }

    @Bean
    @ConditionalOnBean({LoadSocialIdentity.class, ResolveSocialUser.class})
    @ConditionalOnProperty(prefix = "identity-hub.social-login", name = "enabled", havingValue = "true")
    public SocialLogin socialLoginUseCase(
            LoadSocialIdentity loadSocialIdentity,
            ResolveSocialUser resolveSocialUser,
            TokenServicePort tokenServicePort,
            SocialProviderPolicyPort socialProviderPolicyPort
    ) {
        return new SocialLoginUseCase(loadSocialIdentity, resolveSocialUser, tokenServicePort, socialProviderPolicyPort);
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
}
