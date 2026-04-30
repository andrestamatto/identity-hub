package br.dev.andrestamatto.identityhub.application.ports;

import br.dev.andrestamatto.identityhub.application.usecase.SocialLoginInput;
import br.dev.andrestamatto.identityhub.domain.model.SocialIdentity;

public interface LoadSocialIdentity {
    SocialIdentity load(SocialLoginInput socialLoginInput);
}
