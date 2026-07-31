package br.dev.andrestamatto.identityhub.clientapplication.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class BffSettingsTest {

    @Test
    void acceptsExactHttpsRedirectsInProduction() {
        var settings = BffSettings.create(
                List.of("https://app.example.com/login/oauth2/code/identityhub"),
                BrowserTransportPolicy.PRODUCTION);

        assertThat(settings.type()).isEqualTo(ApplicationClientType.BFF);
        assertThat(settings.redirectUris())
                .extracting(BrowserRedirectUri::value)
                .containsExactly("https://app.example.com/login/oauth2/code/identityhub");
    }

    @Test
    void acceptsHttpOnlyForDevelopmentLoopback() {
        assertThat(BffSettings.create(
                        List.of("http://127.0.0.1:8081/login/oauth2/code/identityhub"),
                        BrowserTransportPolicy.DEVELOPMENT))
                .isNotNull();

        assertThatThrownBy(() -> BffSettings.create(
                        List.of("http://app.example.test/login/oauth2/code/identityhub"),
                        BrowserTransportPolicy.DEVELOPMENT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BffSettings.create(
                        List.of("http://127.0.0.1:8081/login/oauth2/code/identityhub"),
                        BrowserTransportPolicy.PRODUCTION))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsWildcardDuplicateOrEmptyRedirects() {
        assertThatThrownBy(() -> BffSettings.create(
                        List.of(), BrowserTransportPolicy.PRODUCTION))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BffSettings.create(
                        List.of(
                                "https://app.example.com/callback",
                                "https://app.example.com/callback"),
                        BrowserTransportPolicy.PRODUCTION))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BffSettings.create(
                        List.of("https://*.example.com/callback"),
                        BrowserTransportPolicy.PRODUCTION))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
