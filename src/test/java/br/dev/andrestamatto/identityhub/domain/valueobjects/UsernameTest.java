package br.dev.andrestamatto.identityhub.domain.valueobjects;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UsernameTest {

    @Test
    public void shouldCreateUsernameSuccessfully() {
        assertDoesNotThrow(() -> Username.create("test@test.com"));
        assertDoesNotThrow(() -> Username.create("42779846062", UsernameType.CPF));
        assertDoesNotThrow(() -> Username.create("nonNullAndNonBlankText", UsernameType.ID));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenValueAndTypeAreNull() {
        assertThrows(IllegalArgumentException.class, () -> Username.create(null, null));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenValueIsNull() {
        assertThrows(IllegalArgumentException.class, () -> Username.create(null, UsernameType.ID));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenIdValueIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> Username.create("", UsernameType.ID));
        assertThrows(IllegalArgumentException.class, () -> Username.create(" ", UsernameType.ID));
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
    public void shouldThrowIllegalArgumentExceptionWhenCpfIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Username.create("invalidCPF", UsernameType.CPF));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenSsnIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Username.create("invalidSSN", UsernameType.SSN));
    }
}
