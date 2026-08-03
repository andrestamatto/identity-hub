package br.dev.andrestamatto.identityhub.integration.autoconfigure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

final class IdentityHubPublicAuthorityClaims {

    private static final int MAXIMUM_AUTHORITIES = 100;
    private static final int MAXIMUM_AUTHORITY_LENGTH = 255;
    private static final Pattern SAFE_AUTHORITY =
            Pattern.compile("^[A-Za-z0-9](?:[A-Za-z0-9._:/-]*[A-Za-z0-9])?$");

    private IdentityHubPublicAuthorityClaims() {
    }

    static Optional<List<String>> scopes(Object claim) {
        if (!(claim instanceof String scope)) {
            return Optional.empty();
        }
        if (scope.isEmpty()) {
            return Optional.of(List.of());
        }
        return validValues(List.of(scope.split(" ", -1)), false);
    }

    static Optional<List<String>> roles(Object claim) {
        if (!(claim instanceof Collection<?> roles)) {
            return Optional.empty();
        }
        return validValues(roles, true);
    }

    private static Optional<List<String>> validValues(Collection<?> values, boolean roles) {
        if (values.size() > MAXIMUM_AUTHORITIES) {
            return Optional.empty();
        }
        Set<String> distinctValues = new HashSet<>();
        var validatedValues = new ArrayList<String>();
        for (var value : values) {
            if (!(value instanceof String authority)
                    || authority.length() > MAXIMUM_AUTHORITY_LENGTH
                    || !SAFE_AUTHORITY.matcher(authority).matches()
                    || !distinctValues.add(authority)
                    || (roles && authority.startsWith("PLATFORM_"))) {
                return Optional.empty();
            }
            validatedValues.add(authority);
        }
        return Optional.of(List.copyOf(validatedValues));
    }
}
