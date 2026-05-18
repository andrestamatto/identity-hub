package br.dev.andrestamatto.identityhub.support;

import br.dev.andrestamatto.identityhub.domain.valueobjects.Credentials;
import br.dev.andrestamatto.identityhub.domain.valueobjects.RawPassword;
import br.dev.andrestamatto.identityhub.domain.valueobjects.Username;

public final class CredentialsTestData {

    private CredentialsTestData() {
    }

    public static Credentials valid() {
        return new Credentials(
                new Username("user1@identityhub.com"),
                new RawPassword("123456")
        );
    }

    public static Credentials with(String username, String rawPassword) {
        return new Credentials(
                new Username(username),
                new RawPassword(rawPassword)
        );
    }

}
