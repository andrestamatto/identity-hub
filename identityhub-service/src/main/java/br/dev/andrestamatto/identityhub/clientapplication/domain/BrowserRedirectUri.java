package br.dev.andrestamatto.identityhub.clientapplication.domain;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;

public record BrowserRedirectUri(String value) {

    private static final int MAX_LENGTH = 2048;

    public BrowserRedirectUri {
        Objects.requireNonNull(value);
        if (value.isBlank() || value.length() > MAX_LENGTH || value.contains("*")) {
            throw new IllegalArgumentException("Invalid browser redirect URI");
        }
        var uri = parse(value);
        var scheme = uri.getScheme() == null
                ? ""
                : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!(scheme.equals("https") || scheme.equals("http"))
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getFragment() != null) {
            throw new IllegalArgumentException("Invalid browser redirect URI");
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

    public String origin() {
        var uri = URI.create(value);
        var host = uri.getHost().contains(":") ? "[" + uri.getHost() + "]" : uri.getHost();
        return uri.getScheme().toLowerCase(Locale.ROOT) + "://" + host.toLowerCase(Locale.ROOT)
                + (uri.getPort() < 0 ? "" : ":" + uri.getPort());
    }

    private static URI parse(String value) {
        try {
            return URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid browser redirect URI", exception);
        }
    }
}
