package br.dev.andrestamatto.identityhub.infrastructure.social.oauth2;

import br.dev.andrestamatto.identityhub.application.usecase.dto.SocialLoginCommand;
import br.dev.andrestamatto.identityhub.domain.model.SocialIdentity;
import br.dev.andrestamatto.identityhub.domain.model.SocialProvider;

public interface OAuth2ProviderClient {
    SocialProvider provider();
    SocialIdentity fetchIdentity(SocialLoginCommand input);
}
