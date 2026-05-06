package br.dev.andrestamatto.identityhub.infrastructure.config;

import br.dev.andrestamatto.identityhub.application.exception.IdentitySourceUnavailableException;
import br.dev.andrestamatto.identityhub.application.ports.LoadSocialIdentityPort;
import br.dev.andrestamatto.identityhub.application.ports.ResolveSocialUserPort;
import br.dev.andrestamatto.identityhub.application.ports.SocialProviderPolicyPort;
import br.dev.andrestamatto.identityhub.application.ports.TokenServicePort;
import br.dev.andrestamatto.identityhub.application.result.AuthenticationResult;
import br.dev.andrestamatto.identityhub.application.result.AuthorizationResult;
import br.dev.andrestamatto.identityhub.application.usecase.SocialLoginUseCase;
import br.dev.andrestamatto.identityhub.application.usecase.port.in.SocialLoginUseCasePort;
import br.dev.andrestamatto.identityhub.infrastructure.mapper.PropertiesSocialProviderPolicyMapper;
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
    public SocialProviderPolicyPort socialProviderPolicyPort(
            PropertiesSocialProviderPolicyMapper propertiesSocialProviderPolicyMapper,
            IdentityHubSocialLoginProperties socialLoginProperties
    ) {
        return new PropertiesSocialProviderPolicyAdapter(propertiesSocialProviderPolicyMapper, socialLoginProperties);
    }

    @Bean
    @ConditionalOnBean({LoadSocialIdentityPort.class, ResolveSocialUserPort.class})
    @ConditionalOnProperty(prefix = "identity-hub.social-login", name = "enabled", havingValue = "true")
    public SocialLoginUseCasePort socialLoginUseCase(
            LoadSocialIdentityPort loadSocialIdentityPort,
            ResolveSocialUserPort resolveSocialUserPort,
            TokenServicePort tokenServicePort,
            SocialProviderPolicyPort socialProviderPolicyPort
    ) {
        return new SocialLoginUseCase(loadSocialIdentityPort, resolveSocialUserPort, tokenServicePort, socialProviderPolicyPort);
    }

    @Bean
    @ConditionalOnMissingBean(SocialLoginUseCasePort.class)
    public SocialLoginUseCasePort unavailableSocialLoginUseCase(
            IdentityHubSocialLoginProperties socialLoginProperties
    ) {
        return new SocialLoginUseCasePort() {
            @Override
            public AuthorizationResult requestAuthorization(String socialProvider) {
                throw unavailableSocialLoginException(socialLoginProperties);
            }

            @Override
            public AuthenticationResult execute(String socialProvider, String authorizationCode, String redirectUri) {
                throw unavailableSocialLoginException(socialLoginProperties);
            }
        };
    }

    private IdentitySourceUnavailableException unavailableSocialLoginException(IdentityHubSocialLoginProperties socialLoginProperties) {
        if (!socialLoginProperties.enabled()) {
            return new IdentitySourceUnavailableException("Social login is disabled in configuration.");
        }
        return new IdentitySourceUnavailableException(
                "Social login is enabled, but LoadSocialIdentityPort and/or ResolveSocialUserPort is not configured."
        );
    }
}
