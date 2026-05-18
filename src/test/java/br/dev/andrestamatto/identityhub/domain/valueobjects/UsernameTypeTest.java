package br.dev.andrestamatto.identityhub.domain.valueobjects;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UsernameTypeTest {

    @Test
    public void shouldValidateEmailTypeSuccessfully() {
        assertTrue(UsernameType.EMAIL.validate("test@email.com"));
        assertTrue(UsernameType.EMAIL.validate("test@email.info"));
    }

    @Test
    public void shouldFailEmailTypeValidation() {
        shouldFailUsernameTypeValidation(UsernameType.EMAIL);
        assertFalse(UsernameType.EMAIL.validate("non-email-text"));
    }

    @Test
    public void shouldValidatePhoneTypeSuccessfully() {
        assertTrue(UsernameType.PHONE.validate("+5511999998888"));
        assertTrue(UsernameType.PHONE.validate("11999998888"));
    }

    @Test
    public void shouldFailPhoneTypeValidation() {
        shouldFailUsernameTypeValidation(UsernameType.PHONE);
        assertFalse(UsernameType.PHONE.validate("text"));
        assertFalse(UsernameType.PHONE.validate("0123456789"));
        assertFalse(UsernameType.PHONE.validate("+"));
    }

    @Test
    public void shouldValidateExternalIdTypeSuccessfully() {
        assertTrue(UsernameType.EXTERNAL_ID.validate("1234567890"));
        assertTrue(UsernameType.EXTERNAL_ID.validate("  id  "));
    }

    @Test
    public void shouldFailExternalIdTypeValidation() {
        shouldFailUsernameTypeValidation(UsernameType.EXTERNAL_ID);
    }

    private void shouldFailUsernameTypeValidation(UsernameType usernameType) {
        assertFalse(usernameType.validate(null));
        assertFalse(usernameType.validate(""));
        assertFalse(usernameType.validate(" "));
    }

}
