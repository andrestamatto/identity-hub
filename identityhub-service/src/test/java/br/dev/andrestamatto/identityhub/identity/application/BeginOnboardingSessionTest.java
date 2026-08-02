package br.dev.andrestamatto.identityhub.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.dev.andrestamatto.identityhub.identity.domain.OnboardingDigest;
import br.dev.andrestamatto.identityhub.identity.domain.OnboardingSession;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BeginOnboardingSessionTest {

    private static final UUID APPLICATION_ID = UUID.randomUUID();
    private static final UUID MACHINE_CLIENT_ID = UUID.randomUUID();
    private static final UUID BROWSER_CLIENT_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-01T20:00:00Z");
    private static final String SESSION_ID = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    private final OnboardingOriginResolver originResolver =
            org.mockito.Mockito.mock(OnboardingOriginResolver.class);
    private final OnboardingSessionRepository repository =
            org.mockito.Mockito.mock(OnboardingSessionRepository.class);
    private final BeginOnboardingSession begin = new BeginOnboardingSession(
            originResolver,
            repository,
            Clock.fixed(NOW, ZoneOffset.UTC),
            () -> SESSION_ID);

    @BeforeEach
    void validOriginAndPersistence() {
        when(originResolver.resolve(MACHINE_CLIENT_ID, BROWSER_CLIENT_ID, redirectUri()))
                .thenReturn(APPLICATION_ID);
        when(repository.saveOrFind(any())).thenAnswer(invocation ->
                new OnboardingSessionRepository.SaveResult(
                        invocation.getArgument(0, OnboardingSession.class), true));
    }

    @Test
    void createsBoundSessionWithoutExposingAcquisition() {
        var result = begin.execute(command());

        assertThat(result.sessionId()).isEqualTo(SESSION_ID);
        assertThat(result.expiresAt()).isEqualTo(NOW.plusSeconds(600));
        assertThat(result.created()).isTrue();
        verify(originResolver).resolve(MACHINE_CLIENT_ID, BROWSER_CLIENT_ID, redirectUri());
        verify(repository).saveOrFind(any(OnboardingSession.class));
    }

    @Test
    void returnsStableReplayAndRejectsIdempotencyCollision() {
        var first = begin.execute(command());
        when(repository.saveOrFind(any())).thenAnswer(invocation ->
                new OnboardingSessionRepository.SaveResult(
                        invocation.getArgument(0, OnboardingSession.class), false));

        var replay = begin.execute(command());

        assertThat(replay.sessionId()).isEqualTo(first.sessionId());
        assertThat(replay.created()).isFalse();

        var conflicting = org.mockito.Mockito.mock(OnboardingSession.class);
        when(conflicting.requestDigest()).thenReturn(new OnboardingDigest("f".repeat(64)));
        when(repository.saveOrFind(any())).thenReturn(
                new OnboardingSessionRepository.SaveResult(conflicting, false));
        assertThatThrownBy(() -> begin.execute(command()))
                .isInstanceOf(OnboardingSessionConflictException.class);
    }

    @Test
    void rejectsInvalidInputBeforePersistence() {
        var invalid = new BeginOnboardingSession.Command(
                MACHINE_CLIENT_ID,
                BROWSER_CLIENT_ID,
                "order 123 contains spaces",
                redirectUri(),
                "plain-verifier",
                "short",
                "onboarding-test");

        assertThatThrownBy(() -> begin.execute(invalid))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).saveOrFind(any());
    }

    private BeginOnboardingSession.Command command() {
        return new BeginOnboardingSession.Command(
                MACHINE_CLIENT_ID,
                BROWSER_CLIENT_ID,
                "order-2026-0001",
                redirectUri(),
                "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
                "purchase-2026-0001",
                "onboarding-test");
    }

    private String redirectUri() {
        return "https://app.example.com/auth/callback";
    }
}
