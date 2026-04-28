package br.dev.andrestamatto.identityhub.infrastructure.security.config;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public record ConfiguredAccessRule(String pattern, AccessType type, String[] values) {

    public static Optional<ConfiguredAccessRule> from(IdentityHubSecurityProperties.Rule rule) {
        if (rule == null || rule.pattern() == null || rule.pattern().isBlank() || rule.access() == null || rule.access().isBlank()) {
            return Optional.empty();
        }

        String pattern = rule.pattern().trim();
        String access = rule.access().trim();
        String normalized = access.toUpperCase(Locale.ROOT);

        if ("PERMIT_ALL".equals(normalized)) {
            return Optional.of(new ConfiguredAccessRule(pattern, AccessType.PERMIT_ALL, new String[0]));
        }
        if ("DENY_ALL".equals(normalized)) {
            return Optional.of(new ConfiguredAccessRule(pattern, AccessType.DENY_ALL, new String[0]));
        }
        if ("AUTHENTICATED".equals(normalized)) {
            return Optional.of(new ConfiguredAccessRule(pattern, AccessType.AUTHENTICATED, new String[0]));
        }
        if (normalized.startsWith("ROLE:")) {
            return Optional.of(new ConfiguredAccessRule(pattern, AccessType.ROLE, splitSingleValue(access, "ROLE:")));
        }
        if (normalized.startsWith("PERM:")) {
            return Optional.of(new ConfiguredAccessRule(pattern, AccessType.PERM, splitSingleValue(access, "PERM:")));
        }
        if (normalized.startsWith("ANY_ROLE:")) {
            return Optional.of(new ConfiguredAccessRule(pattern, AccessType.ANY_ROLE, splitValues(access.substring("ANY_ROLE:".length()))));
        }
        if (normalized.startsWith("ANY_PERM:")) {
            return Optional.of(new ConfiguredAccessRule(pattern, AccessType.ANY_PERM, splitValues(access.substring("ANY_PERM:".length()))));
        }
        if (normalized.startsWith("ANY_AUTHORITY:")) {
            return Optional.of(new ConfiguredAccessRule(pattern, AccessType.ANY_AUTHORITY, splitValues(access.substring("ANY_AUTHORITY:".length()))));
        }
        if (normalized.startsWith("ALL_ROLE:")) {
            return Optional.of(new ConfiguredAccessRule(pattern, AccessType.ALL_ROLE, splitValues(access.substring("ALL_ROLE:".length()))));
        }
        if (normalized.startsWith("ALL_PERM:")) {
            return Optional.of(new ConfiguredAccessRule(pattern, AccessType.ALL_PERM, splitValues(access.substring("ALL_PERM:".length()))));
        }
        if (normalized.startsWith("HAS_IP:")) {
            return Optional.of(new ConfiguredAccessRule(pattern, AccessType.HAS_IP, splitValuesRaw(access.substring("HAS_IP:".length()))));
        }

        throw new IllegalStateException("Unsupported access rule: " + access + " for pattern " + pattern);
    }

    private static String[] splitSingleValue(String access, String prefix) {
        String[] values = splitValues(access.substring(prefix.length()));
        if (values.length != 1) {
            throw new IllegalStateException("Access rule requires exactly one value: " + access);
        }
        return values;
    }

    private static String[] splitValues(String rawValues) {
        String[] values = Arrays.stream(rawValues.split(","))
                .map(String::trim)
                .map(value -> value.toUpperCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .toArray(String[]::new);
        if (values.length == 0) {
            throw new IllegalStateException("Access rule requires at least one value: " + rawValues);
        }
        return values;
    }

    private static String[] splitValuesRaw(String rawValues) {
        String[] values = Arrays.stream(rawValues.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toArray(String[]::new);
        if (values.length == 0) {
            throw new IllegalStateException("Access rule requires at least one value: " + rawValues);
        }
        return values;
    }
}
