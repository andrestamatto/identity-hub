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
                SpaTransportPolicy.PRODUCTION);

        assertThat(settings.type()).isEqualTo(ApplicationClientType.SPA);
        assertThat(settings.redirectUris())
                .extracting(SpaRedirectUri::value)
                .containsExactly("https://app.example.com/auth/callback");
    }

    @Test
    void acceptsHttpOnlyForDevelopmentLoopback() {
        assertThat(SpaSettings.create(
                        List.of("http://127.0.0.1:5173/auth/callback"),
                        List.of("http://127.0.0.1:5173"),
                        SpaTransportPolicy.DEVELOPMENT))
                .isNotNull();

        assertThatThrownBy(() -> SpaSettings.create(
                        List.of("http://app.example.test/auth/callback"),
                        List.of("http://app.example.test"),
                        SpaTransportPolicy.DEVELOPMENT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SpaSettings.create(
                        List.of("http://127.0.0.1:5173/auth/callback"),
                        List.of("http://127.0.0.1:5173"),
                        SpaTransportPolicy.PRODUCTION))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsWildcardUnsafeOriginAndUndeclaredRedirectOrigin() {
        assertThatThrownBy(() -> SpaSettings.create(
                        List.of("https://*.example.com/callback"),
                        List.of("https://*.example.com"),
                        SpaTransportPolicy.PRODUCTION))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SpaSettings.create(
                        List.of("https://login.example.com/callback"),
                        List.of("https://app.example.com/path"),
                        SpaTransportPolicy.PRODUCTION))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SpaSettings.create(
                        List.of("https://login.example.com/callback"),
                        List.of("https://app.example.com"),
                        SpaTransportPolicy.PRODUCTION))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDuplicateOrEmptyEndpointLists() {
        assertThatThrownBy(() -> SpaSettings.create(
                        List.of(),
                        List.of("https://app.example.com"),
                        SpaTransportPolicy.PRODUCTION))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SpaSettings.create(
                        List.of(
                                "https://app.example.com/callback",
                                "https://app.example.com/callback"),
                        List.of("https://app.example.com"),
                        SpaTransportPolicy.PRODUCTION))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
