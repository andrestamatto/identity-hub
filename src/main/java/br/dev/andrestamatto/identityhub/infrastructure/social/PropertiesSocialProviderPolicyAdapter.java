package br.dev.andrestamatto.identityhub.infrastructure.social;

import br.dev.andrestamatto.identityhub.application.ports.SocialProviderPolicyPort;
import br.dev.andrestamatto.identityhub.application.ports.dto.SocialProviderPolicy;
import br.dev.andrestamatto.identityhub.infrastructure.config.IdentityHubSocialLoginProperties;
import br.dev.andrestamatto.identityhub.infrastructure.mapper.PropertiesSocialProviderPolicyMapper;

public class PropertiesSocialProviderPolicyAdapter implements SocialProviderPolicyPort {

    private final PropertiesSocialProviderPolicyMapper propertiesSocialProviderPolicyMapper;
    private final IdentityHubSocialLoginProperties socialLoginProperties;

    public PropertiesSocialProviderPolicyAdapter(PropertiesSocialProviderPolicyMapper propertiesSocialProviderPolicyMapper, IdentityHubSocialLoginProperties socialLoginProperties) {
        this.propertiesSocialProviderPolicyMapper = propertiesSocialProviderPolicyMapper;
        this.socialLoginProperties = socialLoginProperties;
    }

    @Override
    public boolean enabled() {
        return this.socialLoginProperties.enabled();
    }

    @Override
    public SocialProviderPolicy getSocialProviderPolicy(String provider) {
        return propertiesSocialProviderPolicyMapper.toSocialProviderPolicy(socialLoginProperties, provider);
    }

}
