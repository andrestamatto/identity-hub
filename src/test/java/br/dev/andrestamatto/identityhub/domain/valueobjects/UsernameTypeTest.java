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
    public void shouldValidateCPFTypeSuccessfully() {
        assertTrue(UsernameType.CPF.validate("42779846062"));
    }

    @Test
    public void shouldFailCPFTypeValidation() {
        shouldFailUsernameTypeValidation(UsernameType.CPF);
        assertFalse(UsernameType.CPF.validate("text"));
        assertFalse(UsernameType.CPF.validate("1234567890"));
        assertFalse(UsernameType.CPF.validate("123456789012"));
    }

    @Test
    public void shouldValidateSSNTypeSuccessfully() {
        assertTrue(UsernameType.SSN.validate("136-10-0215"));
    }

    @Test
    public void shouldFailSSNTypeValidation() {
        shouldFailUsernameTypeValidation(UsernameType.SSN);
        assertFalse(UsernameType.SSN.validate("text"));
        assertFalse(UsernameType.SSN.validate("136100215"));
    }

    @Test
    public void shouldValidateIDTypeSuccessfully() {
        assertTrue(UsernameType.ID.validate("1234567890"));
        assertTrue(UsernameType.ID.validate("  id  "));
    }

    @Test
    public void shouldFailIDTypeValidation() {
        shouldFailUsernameTypeValidation(UsernameType.ID);
    }

    private void shouldFailUsernameTypeValidation(UsernameType usernameType) {
        assertFalse(usernameType.validate(null));
        assertFalse(usernameType.validate(""));
        assertFalse(usernameType.validate(" "));
    }

}
