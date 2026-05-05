package br.dev.andrestamatto.identityhub.infrastructure.social;

import br.dev.andrestamatto.identityhub.application.ports.LoadSocialIdentityPort;
import br.dev.andrestamatto.identityhub.application.usecase.dto.SocialLoginCommand;
import br.dev.andrestamatto.identityhub.domain.model.SocialIdentity;
import br.dev.andrestamatto.identityhub.domain.model.SocialProvider;
import br.dev.andrestamatto.identityhub.infrastructure.social.oauth2.OAuth2ProviderClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OAuth2SocialIdentityLoaderAdapter implements LoadSocialIdentityPort {

    private final Map<SocialProvider, OAuth2ProviderClient> clients;

    public OAuth2SocialIdentityLoaderAdapter(List<OAuth2ProviderClient> clients) {
        this.clients = clients.stream()
                .collect(Collectors.toMap(OAuth2ProviderClient::provider, Function.identity()));
    }

    @Override
    public SocialIdentity load(SocialLoginCommand socialLoginCommand) {
        var provider = SocialProvider.fromString(socialLoginCommand.provider());
        var client = Optional.ofNullable(clients.get(provider))
                .orElseThrow(() -> new IllegalArgumentException("Provider not supported: " + provider.getProviderName()));

        return client.fetchIdentity(socialLoginCommand);
    }
}
