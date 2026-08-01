package br.dev.andrestamatto.identityhub.identity.adapter.in.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationSnapshot;
import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationUnavailableException;
import br.dev.andrestamatto.identityhub.clientapplication.application.GetClientApplicationByIdentifier;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationState;
import br.dev.andrestamatto.identityhub.clientapplication.domain.SelfRegistrationPolicy;
import br.dev.andrestamatto.identityhub.identity.application.BeginLocalRegistration;
import br.dev.andrestamatto.identityhub.identity.application.ConfirmEmailVerification;
import br.dev.andrestamatto.identityhub.identity.application.EmailVerificationRejectedException;
import br.dev.andrestamatto.identityhub.identity.application.EmailVerificationRateLimitException;
import br.dev.andrestamatto.identityhub.identity.application.LocalIdentityVerificationException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PublicIdentityControllerTest {

    private static final UUID APPLICATION_ID =
            UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
    private static final String REGISTRATION_ENDPOINT =
            "/public/v1/applications/auto-radar/local-registrations";
    private static final String ACCEPTED_MESSAGE =
            "If the request is eligible, verification instructions will be sent";

    private final GetClientApplicationByIdentifier getApplication =
            mock(GetClientApplicationByIdentifier.class);
    private final BeginLocalRegistration beginRegistration = mock(BeginLocalRegistration.class);
    private final ConfirmEmailVerification confirmVerification =
            mock(ConfirmEmailVerification.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        var limiter = new InMemoryRegistrationRateLimiter(
                20, Duration.ofMinutes(15), 100, Clock.systemUTC());
        var controller = new PublicIdentityController(
                getApplication,
                beginRegistration,
                confirmVerification,
                limiter,
                PublicResponseTiming.none());
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new PublicIdentityExceptionHandler())
                .build();
        when(getApplication.execute("auto-radar")).thenReturn(application());
    }

    @Test
    void acceptsRegistrationWithoutExposingInternalReferences() throws Exception {
        when(beginRegistration.execute(any())).thenReturn(new BeginLocalRegistration.Result(
                UUID.fromString("fbd31357-31b8-46dc-9ec7-38b0c72d1207"),
                UUID.fromString("1e04b771-df2f-45e1-bc45-f22d46da11b5")));

        mvc.perform(registrationRequest("andre@example.com", "correct-horse-battery"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value(ACCEPTED_MESSAGE))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("fbd31357"))))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void makesDestinationRateLimitIndistinguishableFromAcceptedRequest() throws Exception {
        when(beginRegistration.execute(any())).thenThrow(new EmailVerificationRateLimitException());

        mvc.perform(registrationRequest("andre@example.com", "correct-horse-battery"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value(ACCEPTED_MESSAGE));
    }

    @Test
    void hidesUnknownApplicationBehindRegistrationUnavailable() throws Exception {
        when(getApplication.execute("auto-radar"))
                .thenThrow(new ClientApplicationUnavailableException());

        mvc.perform(registrationRequest("andre@example.com", "correct-horse-battery"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Registration is unavailable"));
    }

    @Test
    void explainsThePasswordPolicyWithoutEchoingCredentials() throws Exception {
        when(beginRegistration.execute(any()))
                .thenThrow(new IllegalArgumentException(
                        "Password does not satisfy the security policy"));

        mvc.perform(registrationRequest("andre@example.com", "too-short"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value(
                        "Use a valid email and a password between 15 and 64 characters"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("too-short"))));
    }

    @Test
    void confirmsEmailWithoutReturningAccountData() throws Exception {
        mvc.perform(post("/public/v1/email-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"challenge.secret\"}"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void rejectsEveryInvalidVerificationProofGenerically() throws Exception {
        org.mockito.Mockito.doThrow(new EmailVerificationRejectedException())
                .when(confirmVerification).execute("invalid.secret");

        mvc.perform(post("/public/v1/email-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"invalid.secret\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "Email verification could not be completed"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("invalid.secret"))));
    }

    @Test
    void rejectsEmptyVerificationProofGenerically() throws Exception {
        mvc.perform(post("/public/v1/email-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "Email verification could not be completed"));
    }

    @Test
    void sanitizesVerificationProviderFailure() throws Exception {
        org.mockito.Mockito.doThrow(new LocalIdentityVerificationException(true))
                .when(confirmVerification).execute("challenge.secret");

        mvc.perform(post("/public/v1/email-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"challenge.secret\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(
                        "Email verification could not be completed at this time"));
    }

    @Test
    void redactsCredentialsFromRequestRepresentations() {
        var registration = new PublicIdentityController.LocalRegistrationRequest(
                "andre@example.com", "correct-horse-battery".toCharArray());
        var verification = new PublicIdentityController.EmailVerificationRequest(
                "challenge.secret");

        org.assertj.core.api.Assertions.assertThat(registration.toString())
                .doesNotContain("andre@example.com", "correct-horse-battery");
        org.assertj.core.api.Assertions.assertThat(verification.toString())
                .doesNotContain("challenge.secret");
        registration.close();
    }

    @Test
    void returnsRetryAfterWhenTheRemoteAddressExhaustsTheEdgeQuota() throws Exception {
        var limiter = new InMemoryRegistrationRateLimiter(
                1, Duration.ofMinutes(15), 100, Clock.systemUTC());
        var controller = new PublicIdentityController(
                getApplication,
                beginRegistration,
                confirmVerification,
                limiter,
                PublicResponseTiming.none());
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new PublicIdentityExceptionHandler())
                .build();

        mvc.perform(registrationRequest("first@example.com", "correct-horse-battery")
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.10");
                            return request;
                        }))
                .andExpect(status().isAccepted());
        mvc.perform(registrationRequest("second@example.com", "correct-horse-battery")
                        .header("X-Forwarded-For", "198.51.100.15")
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.10");
                            return request;
                        }))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            registrationRequest(String email, String password) {
        return post(REGISTRATION_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\""
                        + password + "\"}");
    }

    private ClientApplicationSnapshot application() {
        return new ClientApplicationSnapshot(
                APPLICATION_ID,
                "auto-radar",
                "Auto Radar",
                ClientApplicationState.DRAFT,
                SelfRegistrationPolicy.ENABLED,
                Instant.parse("2026-08-01T10:00:00Z"));
    }
}
