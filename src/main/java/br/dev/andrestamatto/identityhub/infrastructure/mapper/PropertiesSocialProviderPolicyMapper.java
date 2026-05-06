package br.dev.andrestamatto.identityhub.infrastructure.mapper;

import br.dev.andrestamatto.identityhub.application.ports.dto.SocialProviderPolicy;
import br.dev.andrestamatto.identityhub.domain.model.SocialProvider;
import br.dev.andrestamatto.identityhub.infrastructure.config.IdentityHubSocialLoginProperties;
import org.springframework.stereotype.Component;

@Component
public class PropertiesSocialProviderPolicyMapper {

    public SocialProviderPolicy toSocialProviderPolicy(IdentityHubSocialLoginProperties sociaLoginProperties, String provider) {
        IdentityHubSocialLoginProperties.ProviderProperties providerProperties = sociaLoginProperties.getProviderProperties(provider);
        return new SocialProviderPolicy(
                providerProperties.enabled(),
                SocialProvider.fromString(provider),
                providerProperties.baseUri(),
                providerProperties.defaultRedirectUrl(),
                providerProperties.allowedRedirectUrls(),
                toSocialProviderPolicyCredentials(providerProperties.credentials())
        );
    }

    public SocialProviderPolicy.Credentials toSocialProviderPolicyCredentials(IdentityHubSocialLoginProperties.Credentials credentials) {
        return new SocialProviderPolicy.Credentials(
                credentials.clientId(),
                credentials.clientSecret(),
                credentials.tokenUrl(),
                credentials.userInfoUrl(),
                credentials.scopes()
        );
    }
}
