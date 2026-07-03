package br.dev.andrestamatto.identityhub.domain.valueobjects;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UsernameTest {

    @Test
    public void shouldCreateUsernameSuccessfully() {
        assertDoesNotThrow(() -> Username.create("test@test.com"));
        assertDoesNotThrow(() -> Username.create("+5511999998888", UsernameType.PHONE));
    }

    @Test
    public void shouldCreateEmailUsername() {
        var username = Username.email("test@test.com");

        assertEquals("test@test.com", username.value());
        assertEquals(UsernameType.EMAIL, username.usernameType());
    }

    @Test
    public void shouldCreatePhoneUsername() {
        var username = Username.phone("+5511999998888");

        assertEquals("+5511999998888", username.value());
        assertEquals(UsernameType.PHONE, username.usernameType());
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenValueAndTypeAreNull() {
        assertThrows(IllegalArgumentException.class, () -> Username.create(null, null));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenValueIsNull() {
        assertThrows(IllegalArgumentException.class, () -> Username.create(null));
        assertThrows(IllegalArgumentException.class, () -> Username.create(null, UsernameType.EMAIL));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenValueIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> Username.create(""));
        assertThrows(IllegalArgumentException.class, () -> Username.create(" "));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenTypeIsNull() {
        assertThrows(IllegalArgumentException.class, () -> Username.create("test@test.com", null));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenEmailIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Username.email(null));
        assertThrows(IllegalArgumentException.class, () -> Username.email(""));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenPhoneIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Username.phone(null));
        assertThrows(IllegalArgumentException.class, () -> Username.phone(""));
    }
}
