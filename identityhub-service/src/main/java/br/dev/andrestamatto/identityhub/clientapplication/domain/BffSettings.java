package br.dev.andrestamatto.identityhub.clientapplication.domain;

import java.util.List;
import java.util.Objects;

public record BffSettings(List<BrowserRedirectUri> redirectUris)
        implements ApplicationClientSettings {

    private static final int MAX_REDIRECT_URIS = 10;

    public BffSettings {
        redirectUris = List.copyOf(Objects.requireNonNull(redirectUris));
        if (redirectUris.isEmpty()
                || redirectUris.size() > MAX_REDIRECT_URIS
                || redirectUris.stream().distinct().count() != redirectUris.size()) {
            throw new IllegalArgumentException(
                    "BFF redirect URIs must be unique and contain 1 to 10 values");
        }
    }

    public static BffSettings create(
            List<String> redirectUris,
            BrowserTransportPolicy transportPolicy) {
        Objects.requireNonNull(transportPolicy);
        var redirects = Objects.requireNonNull(redirectUris).stream()
                .map(BrowserRedirectUri::new)
                .toList();
        redirects.forEach(transportPolicy::validate);
        return new BffSettings(redirects);
    }

    @Override
    public ApplicationClientType type() {
        return ApplicationClientType.BFF;
    }
}
