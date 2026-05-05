package br.dev.andrestamatto.identityhub.application.ports;

import br.dev.andrestamatto.identityhub.application.usecase.dto.SocialLoginCommand;
import br.dev.andrestamatto.identityhub.domain.model.SocialIdentity;

public interface LoadSocialIdentityPort {
    SocialIdentity load(SocialLoginCommand socialLoginCommand);
}
