package br.dev.andrestamatto.identityhub.domain.valueobjects;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UsernameTest {

    @Test
    public void shouldCreateUsernameSuccessfully() {
        assertDoesNotThrow(() -> new Username("test@test.com"));
        assertDoesNotThrow(() -> new Username("42779846062", UsernameType.CPF));
        assertDoesNotThrow(() -> new Username("nonNullAndNonBlankText", UsernameType.ID));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenValueAndTypeAreNull() {
        assertThrows(IllegalArgumentException.class, () -> new Username(null, null));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenValueIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new Username(null, UsernameType.ID));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenIdValueIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Username("", UsernameType.ID));
        assertThrows(IllegalArgumentException.class, () -> new Username(" ", UsernameType.ID));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenTypeIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new Username("test@test.com", null));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenEmailIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> new Username("invalidEmail"));
        assertThrows(IllegalArgumentException.class, () -> new Username("invalidEmail", UsernameType.EMAIL));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenCpfIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> new Username("invalidCPF", UsernameType.CPF));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenSsnIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> new Username("invalidSSN", UsernameType.SSN));
    }
}
