package br.dev.andrestamatto.identityhub.interfaces.rest;

import br.dev.andrestamatto.identityhub.application.exceptions.UserAlreadyExistsException;
import br.dev.andrestamatto.identityhub.application.exceptions.EmailDeliveryException;
import br.dev.andrestamatto.identityhub.application.exceptions.UserNotFoundException;
import br.dev.andrestamatto.identityhub.application.ports.input.command.ConfirmUserCommand;
import br.dev.andrestamatto.identityhub.application.ports.input.command.RegisterUserCommand;
import br.dev.andrestamatto.identityhub.application.usecase.ConfirmUser;
import br.dev.andrestamatto.identityhub.application.usecase.RegisterUser;
import br.dev.andrestamatto.identityhub.application.usecase.RegisterUserUseCase;
import br.dev.andrestamatto.identityhub.domain.exceptions.UserStatusDoesNotMatchRegistrationConfirmationException;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({GlobalExceptionHandler.class, UserResponseMapper.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegisterUser registerUser;

    @MockBean
    private ConfirmUser confirmUser;

    private RegisterUser mockedRegisterUserUseCase;
    private ConfirmUser mockedConfirmUserUseCase;

    private UserController userController;
    private RegisterUserCommand validUserCommand;
    private UserResponseMapper userResponseMapper;


    @BeforeEach
    public void setup() {
        mockedRegisterUserUseCase = mock(RegisterUserUseCase.class);
        mockedConfirmUserUseCase = mock(ConfirmUser.class);

        userResponseMapper = new UserResponseMapper();
        validUserCommand = new RegisterUserCommand(
                validUsernameString,
                validRawPasswordString
        );

        userController = new UserController(
                mockedRegisterUserUseCase,
                mockedConfirmUserUseCase,
                userResponseMapper
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
                .andExpect(jsonPath("$.httpStatus").value(422))
                .andExpect(jsonPath("$.httpError").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("Username already exists."))
                .andExpect(jsonPath("$.path").value("/users/register"));

    }


    @Test
    public void IH001ShouldReturnCreatedHttpStatusOnSuccessfulUserRegistration() {
        var registeredUser = UserTestData.registered();
        var userResponse = userResponseMapper.from(registeredUser);

        when(mockedRegisterUserUseCase.execute(any())).thenReturn(registeredUser);

        var response = assertDoesNotThrow(() -> userController.register(validUserCommand));

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(userResponse, response.getBody());
    }

    @Test
    void IH002ShouldReturnActiveUserResponseOnSuccessfulUserConfirmation() throws Exception {
        var activeUser = UserTestData.activate(UserTestData.registered());
        when(confirmUser.execute(any(ConfirmUserCommand.class))).thenReturn(activeUser);

        var confirmRequest = MockMvcRequestBuilders.get("/users/confirm")
                .param("username", validUsernameString)
                .param("code", UserTestData.validVerificationCode);

        mockMvc.perform(confirmRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(validUsernameString))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(confirmUser).execute(any(ConfirmUserCommand.class));
    }

    @Test
    void IH002ShouldReturnApiErrorResponseWhenUserIsNotFoundOnConfirmation() throws Exception {
        doThrow(new UserNotFoundException()).when(confirmUser).execute(any());

        var confirmRequest = MockMvcRequestBuilders.get("/users/confirm")
                .param("username", validUsernameString)
                .param("code", UserTestData.validVerificationCode);

        mockMvc.perform(confirmRequest)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.httpStatus").value(404))
                .andExpect(jsonPath("$.httpError").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found."))
                .andExpect(jsonPath("$.path").value("/users/confirm"));
    }

    @Test
    void IH002ShouldReturnApiErrorResponseWhenUserStatusDoesNotMatchConfirmation() throws Exception {
        doThrow(new UserStatusDoesNotMatchRegistrationConfirmationException()).when(confirmUser).execute(any());

        var confirmRequest = MockMvcRequestBuilders.get("/users/confirm")
                .param("username", validUsernameString)
                .param("code", UserTestData.validVerificationCode);

        mockMvc.perform(confirmRequest)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.httpStatus").value(422))
                .andExpect(jsonPath("$.httpError").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("User status doesn't match registration confirmation condition"))
                .andExpect(jsonPath("$.path").value("/users/confirm"));
    }

    @Test
    void IH002ShouldReturnApiErrorResponseWhenRequestArgumentIsInvalid() throws Exception {
        doThrow(new IllegalArgumentException("Invalid confirmation request.")).when(confirmUser).execute(any());

        var confirmRequest = MockMvcRequestBuilders.get("/users/confirm")
                .param("username", validUsernameString)
                .param("code", UserTestData.validVerificationCode);

        mockMvc.perform(confirmRequest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.httpStatus").value(400))
                .andExpect(jsonPath("$.httpError").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid confirmation request."))
                .andExpect(jsonPath("$.path").value("/users/confirm"));
    }

    @Test
    void IH001ShouldReturnApiErrorResponseWhenEmailDeliveryTimesOut() throws Exception {
        when(registerUser.execute(any())).thenThrow(
                new EmailDeliveryException("Email delivery timed out while connecting to the SMTP provider.")
        );

        var content = """
                {
                  "username": "user1@identityhub.com",
                  "rawPassword": "Password@123"
                }
            """;

        var postPerform = MockMvcRequestBuilders.post("/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);

        mockMvc.perform(postPerform)
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.httpStatus").value(502))
                .andExpect(jsonPath("$.httpError").value("Bad Gateway"))
                .andExpect(jsonPath("$.message").value("Email delivery timed out. Please try again later."))
                .andExpect(jsonPath("$.path").value("/users/register"));
    }


}
