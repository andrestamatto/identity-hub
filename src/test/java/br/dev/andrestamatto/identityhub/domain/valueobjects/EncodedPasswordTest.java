package br.dev.andrestamatto.identityhub.domain.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EncodedPasswordTest {

    @Test
    public void shouldCreateEncodedPasswordSuccessfully() {
        assertDoesNotThrow(() -> new EncodedPassword("12345678912345678901"));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenValueIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> new EncodedPassword(""));
        assertThrows(IllegalArgumentException.class, () -> new EncodedPassword(" "));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenValueIsTooShort() {
        assertThrows(IllegalArgumentException.class, () -> new EncodedPassword("1234567891234567890"));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenValueIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new EncodedPassword(null));
    }

}
