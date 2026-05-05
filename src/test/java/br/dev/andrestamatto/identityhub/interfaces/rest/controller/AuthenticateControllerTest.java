package br.dev.andrestamatto.identityhub.interfaces.rest.controller;

import br.dev.andrestamatto.identityhub.application.result.AuthenticationResult;
import br.dev.andrestamatto.identityhub.application.usecase.port.in.PasswordLoginUseCasePort;
import br.dev.andrestamatto.identityhub.domain.model.RawPassword;
import br.dev.andrestamatto.identityhub.interfaces.rest.dto.LoginResponse;
import br.dev.andrestamatto.identityhub.interfaces.rest.mapper.AuthResponseMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthenticateController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthenticateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PasswordLoginUseCasePort login;

    @MockBean
    private AuthResponseMapper authResponseMapper;

    @Test
    void shouldAuthenticateWithIdentityField() throws Exception {
        when(login.execute(eq("user@identityhub.dev"), eq(RawPassword.from("Password@123"))))
                .thenReturn(new AuthenticationResult("token-abc", "Bearer", 3600));
        when(authResponseMapper.toLoginResponse(eq(new AuthenticationResult("token-abc", "Bearer", 3600))))
                .thenReturn(new LoginResponse("token-abc", "Bearer", 3600));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identity":"user@identityhub.dev","password":"Password@123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token-abc"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));

        verify(login).execute("user@identityhub.dev", RawPassword.from("Password@123"));
    }

    @Test
    void shouldAuthenticateWithEmailAliasField() throws Exception {
        when(login.execute(eq("user@identityhub.dev"), eq(RawPassword.from("Password@123"))))
                .thenReturn(new AuthenticationResult("token-xyz", "Bearer", 3600));
        when(authResponseMapper.toLoginResponse(eq(new AuthenticationResult("token-xyz", "Bearer", 3600))))
                .thenReturn(new LoginResponse("token-xyz", "Bearer", 3600));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@identityhub.dev","password":"Password@123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token-xyz"));

        verify(login).execute("user@identityhub.dev", RawPassword.from("Password@123"));
    }

    @Test
    void shouldReturnBadRequestWhenRequiredFieldsAreMissing() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identity":"","password":""}
                                """))
                .andExpect(status().isBadRequest());
    }
}
