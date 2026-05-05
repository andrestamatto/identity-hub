package br.dev.andrestamatto.identityhub.application.ports;

import br.dev.andrestamatto.identityhub.domain.model.SocialIdentity;
import br.dev.andrestamatto.identityhub.domain.model.User;

public interface ResolveSocialUserPort {
    User resolve(SocialIdentity socialIdentity);
}
