package br.dev.andrestamatto.identityhub.domain.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CredentialsTest {

    @Test
    void shouldCreateLoginDataSuccessfully() {
        var username = new Username("test@test.com");
        var password = new RawPassword("Password@123");
        assertDoesNotThrow(() -> new Credentials(username, password));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenUsernameIsNull() {
        var password = new RawPassword("Password@123");
        assertThrows(IllegalArgumentException.class, () -> new Credentials(null, password));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenRawPasswordIsNull() {
        var username = new Username("test@test.com");
        assertThrows(IllegalArgumentException.class, () -> new Credentials(username, null));
    }
}
