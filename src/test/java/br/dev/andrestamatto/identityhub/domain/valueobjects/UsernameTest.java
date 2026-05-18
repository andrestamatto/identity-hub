package br.dev.andrestamatto.identityhub.domain.valueobjects;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UsernameTest {

    @Test
    public void shouldCreateUsernameSuccessfully() {
        assertDoesNotThrow(() -> Username.create("test@test.com"));
        assertDoesNotThrow(() -> Username.create("+5511999998888", UsernameType.PHONE));
        assertDoesNotThrow(() -> Username.create("nonNullAndNonBlankText", UsernameType.EXTERNAL_ID));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenValueAndTypeAreNull() {
        assertThrows(IllegalArgumentException.class, () -> Username.create(null, null));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenValueIsNull() {
        assertThrows(IllegalArgumentException.class, () -> Username.create(null, UsernameType.EXTERNAL_ID));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenIdValueIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> Username.create("", UsernameType.EXTERNAL_ID));
        assertThrows(IllegalArgumentException.class, () -> Username.create(" ", UsernameType.EXTERNAL_ID));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenTypeIsNull() {
        assertThrows(IllegalArgumentException.class, () -> Username.create("test@test.com", null));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenEmailIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Username.create("invalidEmail"));
        assertThrows(IllegalArgumentException.class, () -> Username.create("invalidEmail", UsernameType.EMAIL));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenPhoneIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Username.create("invalidPhone", UsernameType.PHONE));
        assertThrows(IllegalArgumentException.class, () -> Username.create("0123456789", UsernameType.PHONE));
    }
}
