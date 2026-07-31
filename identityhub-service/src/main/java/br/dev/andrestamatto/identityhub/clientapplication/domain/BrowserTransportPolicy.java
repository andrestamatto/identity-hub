package br.dev.andrestamatto.identityhub.clientapplication.domain;

public enum BrowserTransportPolicy {
    DEVELOPMENT,
    PRODUCTION;

    void validate(BrowserRedirectUri redirectUri) {
        if (!redirectUri.usesHttps()
                && (this == PRODUCTION || !redirectUri.usesLoopbackHost())) {
            throw new IllegalArgumentException("Browser redirect URI must use secure transport");
        }
    }

    void validate(WebOrigin origin) {
        if (!origin.usesHttps()
                && (this == PRODUCTION || !origin.usesLoopbackHost())) {
            throw new IllegalArgumentException("Web origin must use secure transport");
        }
    }
}
