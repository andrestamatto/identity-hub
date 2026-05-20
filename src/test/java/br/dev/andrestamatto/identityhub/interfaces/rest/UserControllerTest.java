package br.dev.andrestamatto.identityhub.interfaces.rest;

import br.dev.andrestamatto.identityhub.application.exceptions.UserAlreadyExistsException;
import br.dev.andrestamatto.identityhub.application.ports.input.command.RegisterUserCommand;
import br.dev.andrestamatto.identityhub.application.usecase.RegisterUser;
import br.dev.andrestamatto.identityhub.application.usecase.RegisterUserUseCase;
import br.dev.andrestamatto.identityhub.interfaces.rest.handler.GlobalExceptionHandler;
import br.dev.andrestamatto.identityhub.interfaces.rest.mapper.UserResponseMapper;
import br.dev.andrestamatto.identityhub.support.UserTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static br.dev.andrestamatto.identityhub.support.UserTestData.validRawPasswordString;
import static br.dev.andrestamatto.identityhub.support.UserTestData.validUsernameString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({GlobalExceptionHandler.class, UserResponseMapper.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegisterUser registerUser;

    private RegisterUserCommand validUserCommand;

    @BeforeEach
    public void setup() {
        validUserCommand = new RegisterUserCommand(
                validUsernameString,
                validRawPasswordString
        );
    }

    @Test
    void IH001ShouldReturnApiErrorResponseWhenUsernameAlreadyExists() throws Exception {
        when(registerUser.execute(any())).thenThrow(new UserAlreadyExistsException());

        var content = """
                {
                  "username": "user1@identityhub.com",
                  "rawPassword": "Password@123"
                }
            """;

        var postPerform = MockMvcRequestBuilders.post("/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);

        ResultActions unprocessableEntity = mockMvc.perform(postPerform)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"));

    }


    @Test
    public void IH001ShouldReturnCreatedStatusOnSuccessfulUserRegistration() {
        var mockedRegisterUserUseCase = mock(RegisterUserUseCase.class);
        var userResponseMapper = new UserResponseMapper();
        var registeredUser = UserTestData.registered();
        var registeredUserResponse = userResponseMapper.registeredUserResponseFrom(registeredUser);

        UserController userController = new UserController(mockedRegisterUserUseCase, userResponseMapper);

        when(mockedRegisterUserUseCase.execute(any())).thenReturn(registeredUser);

        var response = assertDoesNotThrow(() -> userController.register(validUserCommand));

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(registeredUserResponse, response.getBody());
    }
}