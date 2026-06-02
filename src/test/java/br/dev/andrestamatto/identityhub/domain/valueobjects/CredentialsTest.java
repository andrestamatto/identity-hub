package br.dev.andrestamatto.identityhub.domain.valueobjects;

import br.dev.andrestamatto.identityhub.support.UserTestData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CredentialsTest {

    @Test
    void shouldCreateLoginDataSuccessfully() {
        var username = UserTestData.validEmailUsername();
        var password = RawPassword.create("Password@123");
        assertDoesNotThrow(() -> new Credentials(username, password));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenUsernameIsNull() {
        var password = RawPassword.create("Password@123");
        assertThrows(IllegalArgumentException.class, () -> new Credentials(null, password));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenRawPasswordIsNull() {
        var username = UserTestData.validEmailUsername();
        assertThrows(IllegalArgumentException.class, () -> new Credentials(username, null));
    }
}
