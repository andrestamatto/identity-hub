package br.dev.andrestamatto.identityhub.application.ports;

import br.dev.andrestamatto.identityhub.application.ports.dto.SocialProviderPolicy;

public interface SocialProviderPolicyPort {
    boolean enabled();
    SocialProviderPolicy getProviderPolicy(String provider);
}
