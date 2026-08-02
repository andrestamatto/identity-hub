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
import br.dev.andrestamatto.identityhub.identity.application.PasswordRecoveryRateLimitException;
import br.dev.andrestamatto.identityhub.identity.application.RequestPasswordRecovery;
import br.dev.andrestamatto.identityhub.identity.application.SelfRegistrationDisabledException;
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
    private static final String RECOVERY_ENDPOINT =
            "/public/v1/applications/auto-radar/password-recoveries";
    private static final String RECOVERY_ACCEPTED_MESSAGE =
            "If the account is eligible, password recovery instructions will be sent";

    private final GetClientApplicationByIdentifier getApplication =
            mock(GetClientApplicationByIdentifier.class);
    private final BeginLocalRegistration beginRegistration = mock(BeginLocalRegistration.class);
    private final ConfirmEmailVerification confirmVerification =
            mock(ConfirmEmailVerification.class);
    private final RequestPasswordRecovery requestPasswordRecovery =
            mock(RequestPasswordRecovery.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        var limiter = new InMemoryRegistrationRateLimiter(
                20, Duration.ofMinutes(15), 100, Clock.systemUTC());
        var recoveryLimiter = new InMemoryPasswordRecoveryRateLimiter(
                20, Duration.ofMinutes(15), 100, Clock.systemUTC());
        var controller = new PublicIdentityController(
                getApplication,
                beginRegistration,
                confirmVerification,
                requestPasswordRecovery,
                limiter,
                recoveryLimiter,
                PublicResponseTiming.none());
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new PublicIdentityExceptionHandler())
                .build();
        when(getApplication.execute("auto-radar")).thenReturn(application());
    }

    @Test
    void acceptsPasswordRecoveryWithoutRevealingAccountEligibility() throws Exception {
        var first = mvc.perform(recoveryRequest("existing@example.test"))
                .andReturn().getResponse();
        var second = mvc.perform(recoveryRequest("unknown@example.test"))
                .andReturn().getResponse();

        org.assertj.core.api.Assertions.assertThat(first.getStatus()).isEqualTo(202);
        org.assertj.core.api.Assertions.assertThat(second.getStatus()).isEqualTo(first.getStatus());
        org.assertj.core.api.Assertions.assertThat(second.getContentAsString())
                .isEqualTo(first.getContentAsString())
                .contains(RECOVERY_ACCEPTED_MESSAGE);
        org.assertj.core.api.Assertions.assertThat(second.getHeader("Cache-Control"))
                .isEqualTo(first.getHeader("Cache-Control"));
    }

    @Test
    void hidesPasswordRecoveryDestinationLimitBehindAcceptedResponse() throws Exception {
        org.mockito.Mockito.doThrow(new PasswordRecoveryRateLimitException())
                .when(requestPasswordRecovery).execute(any());

        mvc.perform(recoveryRequest("andre@example.test"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value(RECOVERY_ACCEPTED_MESSAGE));
    }

    @Test
    void rejectsMalformedRecoveryRequestWithoutEchoingEmail() throws Exception {
        mvc.perform(recoveryRequest("not-an-email"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value("Use a valid email address"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("not-an-email"))));
    }

    @Test
    void hidesUnknownApplicationBehindRecoveryUnavailable() throws Exception {
        when(getApplication.execute("auto-radar"))
                .thenThrow(new ClientApplicationUnavailableException());

        mvc.perform(recoveryRequest("andre@example.test"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Password recovery is unavailable"));
    }

    @Test
    void limitsPasswordRecoveryByRemoteAddressAndIgnoresForwardedSpoofing()
            throws Exception {
        var controller = new PublicIdentityController(
                getApplication,
                beginRegistration,
                confirmVerification,
                requestPasswordRecovery,
                new InMemoryRegistrationRateLimiter(
                        20, Duration.ofMinutes(15), 100, Clock.systemUTC()),
                new InMemoryPasswordRecoveryRateLimiter(
                        1, Duration.ofMinutes(15), 100, Clock.systemUTC()),
                PublicResponseTiming.none());
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new PublicIdentityExceptionHandler())
                .build();

        mvc.perform(recoveryRequest("first@example.test")
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.20");
                            return request;
                        }))
                .andExpect(status().isAccepted());
        mvc.perform(recoveryRequest("second@example.test")
                        .header("X-Forwarded-For", "198.51.100.25")
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.20");
                            return request;
                        }))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"));
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
    void returnsTheSamePublicContractForNewAndExistingAccounts() throws Exception {
        when(beginRegistration.execute(any()))
                .thenReturn(
                        new BeginLocalRegistration.Result(
                                UUID.fromString("fbd31357-31b8-46dc-9ec7-38b0c72d1207"),
                                UUID.fromString("1e04b771-df2f-45e1-bc45-f22d46da11b5")),
                        new BeginLocalRegistration.Result(
                                UUID.fromString("936bc8c5-a66b-4795-b16c-ac375700759e"),
                                UUID.fromString("2ea5eaac-f9e5-4826-9129-a889e6358852")));

        var newAccount = mvc.perform(registrationRequest(
                        "new@example.com", "correct-horse-battery"))
                .andReturn()
                .getResponse();
        var existingAccount = mvc.perform(registrationRequest(
                        "existing@example.com", "correct-horse-battery"))
                .andReturn()
                .getResponse();

        org.assertj.core.api.Assertions.assertThat(existingAccount.getStatus())
                .isEqualTo(newAccount.getStatus());
        org.assertj.core.api.Assertions.assertThat(existingAccount.getContentAsString())
                .isEqualTo(newAccount.getContentAsString());
        org.assertj.core.api.Assertions.assertThat(existingAccount.getHeader("Cache-Control"))
                .isEqualTo(newAccount.getHeader("Cache-Control"));
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
    void hidesDisabledRegistrationBehindTheSameUnavailableResponse() throws Exception {
        when(beginRegistration.execute(any())).thenThrow(new SelfRegistrationDisabledException());

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
        var recovery = new PublicIdentityController.PasswordRecoveryRequest(
                "andre@example.com");

        org.assertj.core.api.Assertions.assertThat(registration.toString())
                .doesNotContain("andre@example.com", "correct-horse-battery");
        org.assertj.core.api.Assertions.assertThat(verification.toString())
                .doesNotContain("challenge.secret");
        org.assertj.core.api.Assertions.assertThat(recovery.toString())
                .doesNotContain("andre@example.com");
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
                requestPasswordRecovery,
                limiter,
                new InMemoryPasswordRecoveryRateLimiter(
                        20, Duration.ofMinutes(15), 100, Clock.systemUTC()),
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
            recoveryRequest(String email) {
        return post(RECOVERY_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}");
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
