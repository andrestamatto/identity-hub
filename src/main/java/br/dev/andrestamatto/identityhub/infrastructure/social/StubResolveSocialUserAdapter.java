package br.dev.andrestamatto.identityhub.infrastructure.social;

import br.dev.andrestamatto.identityhub.application.ports.ResolveSocialUserPort;
import br.dev.andrestamatto.identityhub.domain.model.SocialIdentity;
import br.dev.andrestamatto.identityhub.domain.model.User;
import org.springframework.stereotype.Component;

@Component
public class StubResolveSocialUserAdapter implements ResolveSocialUserPort {
    @Override
    public User resolve(SocialIdentity socialIdentity) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}