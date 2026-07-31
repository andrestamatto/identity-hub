package br.dev.andrestamatto.identityhub.clientapplication.domain;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;

public record WebOrigin(String value) {

    private static final int MAX_LENGTH = 255;

    public WebOrigin {
        Objects.requireNonNull(value);
        if (value.isBlank() || value.length() > MAX_LENGTH || value.contains("*")) {
            throw new IllegalArgumentException("Invalid web origin");
        }
        var uri = parse(value);
        var scheme = uri.getScheme() == null
                ? ""
                : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!(scheme.equals("https") || scheme.equals("http"))
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getRawQuery() != null
                || uri.getFragment() != null
                || (uri.getRawPath() != null && !uri.getRawPath().isEmpty())) {
            throw new IllegalArgumentException("Invalid web origin");
        }
    }

    public boolean usesHttps() {
        return URI.create(value).getScheme().equalsIgnoreCase("https");
    }

    public boolean usesLoopbackHost() {
        var host = URI.create(value).getHost();
        return host.equalsIgnoreCase("localhost")
                || host.equals("127.0.0.1")
                || host.equals("::1");
    }

    private static URI parse(String value) {
        try {
            return URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid web origin", exception);
        }
    }
}
