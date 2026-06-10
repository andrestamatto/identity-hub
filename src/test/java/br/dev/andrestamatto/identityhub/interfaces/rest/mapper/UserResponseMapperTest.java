package br.dev.andrestamatto.identityhub.interfaces.rest.mapper;

import br.dev.andrestamatto.identityhub.domain.valueobjects.UserStatus;
import br.dev.andrestamatto.identityhub.support.UserTestData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class UserResponseMapperTest {

    @Test
    public void shouldMapActiveUserWithoutVerificationTokenToUserResponse() {
        var mapper = new UserResponseMapper();
        var activeUser = UserTestData.activate(UserTestData.registered());

        var response = mapper.from(activeUser);

        assertEquals(UserTestData.validUsernameString, response.username());
        assertEquals(UserStatus.ACTIVE.name(), response.status());
        assertNull(response.verificationMethod());
        assertNull(response.verificationExpiresAt());
    }
}
