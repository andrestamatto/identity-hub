package br.dev.andrestamatto.identityhub.infrastructure.social;

import br.dev.andrestamatto.identityhub.application.ports.SocialProviderPolicyPort;
import br.dev.andrestamatto.identityhub.application.ports.dto.SocialProviderPolicy;
import br.dev.andrestamatto.identityhub.infrastructure.config.IdentityHubSocialLoginProperties;

public class PropertiesSocialProviderPolicyAdapter implements SocialProviderPolicyPort {

    private final IdentityHubSocialLoginProperties socialLoginProperties;

    public PropertiesSocialProviderPolicyAdapter(IdentityHubSocialLoginProperties socialLoginProperties) {
        this.socialLoginProperties = socialLoginProperties;
    }

    @Override
    public boolean enabled() {
        return this.socialLoginProperties.enabled();
    }

    @Override
    public SocialProviderPolicy getProviderPolicy(String provider) {
        IdentityHubSocialLoginProperties.ProviderProperties providerProperties = this.socialLoginProperties.getProviderProperties(provider);
        return new SocialProviderPolicy(
                providerProperties.enabled(),
                providerProperties.defaultRedirectUrl(),
                providerProperties.allowedRedirectUrls()
        );
    }
}
