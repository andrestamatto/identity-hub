package br.dev.andrestamatto.identityhub.clientapplication.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class SpaSettingsTest {

    @Test
    void acceptsExactHttpsEndpointsInProduction() {
        var settings = SpaSettings.create(
                List.of("https://app.example.com/auth/callback"),
                List.of("https://app.example.com"),
                BrowserTransportPolicy.PRODUCTION);

        assertThat(settings.type()).isEqualTo(ApplicationClientType.SPA);
        assertThat(settings.redirectUris())
                .extracting(BrowserRedirectUri::value)
                .containsExactly("https://app.example.com/auth/callback");
    }

    @Test
    void acceptsHttpOnlyForDevelopmentLoopback() {
        assertThat(SpaSettings.create(
                        List.of("http://127.0.0.1:5173/auth/callback"),
                        List.of("http://127.0.0.1:5173"),
                        BrowserTransportPolicy.DEVELOPMENT))
                .isNotNull();

        assertThatThrownBy(() -> SpaSettings.create(
                        List.of("http://app.example.test/auth/callback"),
                        List.of("http://app.example.test"),
                        BrowserTransportPolicy.DEVELOPMENT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SpaSettings.create(
                        List.of("http://127.0.0.1:5173/auth/callback"),
                        List.of("http://127.0.0.1:5173"),
                        BrowserTransportPolicy.PRODUCTION))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsWildcardUnsafeOriginAndUndeclaredRedirectOrigin() {
        assertThatThrownBy(() -> SpaSettings.create(
                        List.of("https://*.example.com/callback"),
                        List.of("https://*.example.com"),
                        BrowserTransportPolicy.PRODUCTION))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SpaSettings.create(
                        List.of("https://login.example.com/callback"),
                        List.of("https://app.example.com/path"),
                        BrowserTransportPolicy.PRODUCTION))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SpaSettings.create(
                        List.of("https://login.example.com/callback"),
                        List.of("https://app.example.com"),
                        BrowserTransportPolicy.PRODUCTION))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDuplicateOrEmptyEndpointLists() {
        assertThatThrownBy(() -> SpaSettings.create(
                        List.of(),
                        List.of("https://app.example.com"),
                        BrowserTransportPolicy.PRODUCTION))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SpaSettings.create(
                        List.of(
                                "https://app.example.com/callback",
                                "https://app.example.com/callback"),
                        List.of("https://app.example.com"),
                        BrowserTransportPolicy.PRODUCTION))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
