package br.dev.andrestamatto.identityhub.domain.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RawPasswordTest {

    @Test
    public void shouldCreateRawPasswordSuccessfully() {
        assertDoesNotThrow(() -> new RawPassword("123456"));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenValueIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> new RawPassword(""));
        assertThrows(IllegalArgumentException.class, () -> new RawPassword(" "));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenValueIsTooShort() {
        assertThrows(IllegalArgumentException.class, () -> new RawPassword("12345"));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenValueIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new RawPassword(null));
    }

}
