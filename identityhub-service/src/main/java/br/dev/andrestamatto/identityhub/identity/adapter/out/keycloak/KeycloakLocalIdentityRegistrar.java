package br.dev.andrestamatto.identityhub.identity.adapter.out.keycloak;

import br.dev.andrestamatto.identityhub.identity.application.LocalIdentityRegistrar;
import br.dev.andrestamatto.identityhub.identity.application.LocalIdentityRegistration;
import br.dev.andrestamatto.identityhub.identity.application.LocalIdentityRegistrationException;
import br.dev.andrestamatto.identityhub.identity.application.LocalIdentityRegistrationFailureCode;
import br.dev.andrestamatto.identityhub.identity.application.PendingLocalIdentity;
import br.dev.andrestamatto.identityhub.identity.domain.UserAccountRef;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class KeycloakLocalIdentityRegistrar implements LocalIdentityRegistrar {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI baseUri;
    private final String realm;
    private final String managementClientId;
    private final String managementClientSecret;

    public KeycloakLocalIdentityRegistrar(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            URI baseUri,
            String realm,
            String managementClientId,
            String managementClientSecret) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.baseUri = Objects.requireNonNull(baseUri);
        this.realm = requireText(realm, "Realm");
        this.managementClientId = requireText(managementClientId, "Management client id");
        this.managementClientSecret = requireText(
                managementClientSecret, "Management client secret");
    }

    @Override
    public LocalIdentityRegistration register(PendingLocalIdentity identity) {
        Objects.requireNonNull(identity);
        try {
            var accessToken = requestManagementToken();
            var existing = findUser(identity, accessToken);
            if (existing != null) {
                return registration(existing, false);
            }
            return createUser(identity, accessToken);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw retryable(LocalIdentityRegistrationFailureCode.PROVIDER_UNAVAILABLE, exception);
        } catch (IOException exception) {
            throw retryable(LocalIdentityRegistrationFailureCode.PROVIDER_UNAVAILABLE, exception);
        } catch (LocalIdentityRegistrationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalidResponse();
        }
    }

    private String requestManagementToken() throws IOException, InterruptedException {
        var credentials = Base64.getEncoder().encodeToString(
                (managementClientId + ":" + managementClientSecret)
                        .getBytes(StandardCharsets.UTF_8));
        var request = HttpRequest.newBuilder(tokenUri())
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        ensureSuccess(
                response.statusCode(),
                LocalIdentityRegistrationFailureCode.MANAGEMENT_AUTHENTICATION_REJECTED);
        try {
            return objectMapper.readTree(response.body()).required("access_token").asString();
        } catch (RuntimeException exception) {
            throw invalidResponse();
        }
    }

    private JsonNode findUser(PendingLocalIdentity identity, String accessToken)
            throws IOException, InterruptedException {
        var response = sendAuthorized(
                HttpRequest.newBuilder(userLookupUri(identity)).GET(), accessToken);
        ensureSuccess(
                response.statusCode(),
                LocalIdentityRegistrationFailureCode.MANAGEMENT_REQUEST_REJECTED);
        try {
            var users = objectMapper.readValue(
                    response.body(), new TypeReference<List<JsonNode>>() { });
            if (users.size() > 1) {
                throw LocalIdentityRegistrationException.permanent(
                        LocalIdentityRegistrationFailureCode.IDENTITY_CONFLICT, null);
            }
            return users.isEmpty() ? null : users.getFirst();
        } catch (LocalIdentityRegistrationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalidResponse();
        }
    }

    private LocalIdentityRegistration createUser(
            PendingLocalIdentity identity, String accessToken)
            throws IOException, InterruptedException {
        var password = identity.password().copy();
        byte[] body = null;
        try {
            body = representation(identity, password);
            var response = sendAuthorized(
                    HttpRequest.newBuilder(userCollectionUri())
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofByteArray(body)),
                    accessToken);
            if (response.statusCode() == 409) {
                var racedUser = findUser(identity, accessToken);
                if (racedUser != null) {
                    return registration(racedUser, false);
                }
                throw LocalIdentityRegistrationException.permanent(
                        LocalIdentityRegistrationFailureCode.IDENTITY_CONFLICT, null);
            }
            ensureSuccess(
                    response.statusCode(),
                    LocalIdentityRegistrationFailureCode.MANAGEMENT_REQUEST_REJECTED);
            return new LocalIdentityRegistration(
                    new UserAccountRef(userIdFrom(response)), true);
        } finally {
            Arrays.fill(password, '\0');
            if (body != null) {
                Arrays.fill(body, (byte) 0);
            }
        }
    }

    private byte[] representation(PendingLocalIdentity identity, char[] password) {
        var credential = new LinkedHashMap<String, Object>();
        credential.put("type", "password");
        credential.put("value", password);
        credential.put("temporary", false);

        var user = new LinkedHashMap<String, Object>();
        user.put("username", identity.email().normalizedValue());
        user.put("email", identity.email().contactValue());
        user.put("enabled", false);
        user.put("emailVerified", false);
        user.put("credentials", List.of(credential));
        user.put("attributes", Map.of("identityhub.local-identity", List.of("true")));
        return objectMapper.writeValueAsBytes(user);
    }

    private LocalIdentityRegistration registration(JsonNode user, boolean created) {
        try {
            return new LocalIdentityRegistration(
                    new UserAccountRef(UUID.fromString(user.required("id").asString())), created);
        } catch (RuntimeException exception) {
            throw invalidResponse();
        }
    }

    private UUID userIdFrom(HttpResponse<String> response) {
        try {
            var location = response.headers().firstValue("Location").orElseThrow();
            var path = URI.create(location).getPath();
            return UUID.fromString(path.substring(path.lastIndexOf('/') + 1));
        } catch (RuntimeException exception) {
            throw invalidResponse();
        }
    }

    private HttpResponse<String> sendAuthorized(
            HttpRequest.Builder request, String accessToken)
            throws IOException, InterruptedException {
        return httpClient.send(
                request.timeout(REQUEST_TIMEOUT)
                        .header("Authorization", "Bearer " + accessToken)
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI tokenUri() {
        return baseUri.resolve("/realms/" + encode(realm)
                + "/protocol/openid-connect/token");
    }

    private URI userCollectionUri() {
        return baseUri.resolve("/admin/realms/" + encode(realm) + "/users");
    }

    private URI userLookupUri(PendingLocalIdentity identity) {
        return URI.create(userCollectionUri() + "?username="
                + encode(identity.email().normalizedValue()) + "&exact=true");
    }

    private static void ensureSuccess(
            int statusCode, LocalIdentityRegistrationFailureCode permanentFailureCode) {
        if (statusCode >= 200 && statusCode < 300) {
            return;
        }
        if (statusCode == 400 || statusCode == 401 || statusCode == 403) {
            throw LocalIdentityRegistrationException.permanent(
                    permanentFailureCode,
                    new IllegalStateException("Identity provider rejected management request"));
        }
        throw retryable(
                LocalIdentityRegistrationFailureCode.PROVIDER_UNAVAILABLE,
                new IllegalStateException("Identity provider management request failed"));
    }

    private static LocalIdentityRegistrationException retryable(
            LocalIdentityRegistrationFailureCode failureCode, Throwable cause) {
        return LocalIdentityRegistrationException.retryable(failureCode, cause);
    }

    private static LocalIdentityRegistrationException invalidResponse() {
        return retryable(
                LocalIdentityRegistrationFailureCode.INVALID_PROVIDER_RESPONSE,
                new IllegalStateException("Invalid identity provider response"));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value;
    }
}
