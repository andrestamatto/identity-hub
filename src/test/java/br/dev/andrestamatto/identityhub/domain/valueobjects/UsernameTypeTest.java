package br.dev.andrestamatto.identityhub.domain.valueobjects;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UsernameTypeTest {

    @Test
    public void shouldCreateUsernameTypeFromText() {
        assertEquals(UsernameType.EMAIL, UsernameType.create("email"));
        assertEquals(UsernameType.PHONE, UsernameType.create("PHONE"));
    }

    @Test
    public void shouldRejectUnknownUsernameType() {
        assertThrows(IllegalArgumentException.class, () -> UsernameType.create("unknown"));
    }

}
