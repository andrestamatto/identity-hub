package br.dev.andrestamatto.identityhub.application.usecase;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

public class ConfirmUserRegistrationUseCaseTest {


    private ConfirmUser mockedConfirmUser;

    @BeforeEach
    public void setup() {
        mockedConfirmUser = mock(ConfirmUser.class);
    }

    @Test
    public void IH002ShouldConfirmUserRegistrationGivenCorrectCodeOnFirstUserAccess() {

    }

    @Test
    public void IH002ShouldRejectConfirmationOnGivenWrongCodeOnFirstUserAccess() {

    }

    @Test
    public void IH002ShouldResendConfirmationCodeWhenRequested() {}

    @Test
    public void IH002ShouldInvalidateAllPreviousConfirmationCodesWhenNewOneIsRequested() {

    }

}
