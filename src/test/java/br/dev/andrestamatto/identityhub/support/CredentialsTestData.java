package br.dev.andrestamatto.identityhub.support;

import br.dev.andrestamatto.identityhub.domain.valueobjects.Credentials;
import br.dev.andrestamatto.identityhub.domain.valueobjects.RawPassword;

public final class CredentialsTestData {

    private CredentialsTestData() {
    }

    public static Credentials valid() {
        return new Credentials(
                UserTestData.validEmailUsername(),
                RawPassword.create(UserTestData.validRawPasswordString)
        );
    }

}
