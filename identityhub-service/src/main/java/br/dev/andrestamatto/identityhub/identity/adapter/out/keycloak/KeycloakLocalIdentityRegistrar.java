package br.dev.andrestamatto.identityhub.identity.adapter.out.keycloak;

import br.dev.andrestamatto.identityhub.identity.application.LocalIdentityRegistrar;
import br.dev.andrestamatto.identityhub.identity.application.LocalIdentityRegistration;
import br.dev.andrestamatto.identityhub.identity.application.LocalIdentityRegistrationException;
import br.dev.andrestamatto.identityhub.identity.application.LocalIdentityRegistrationFailureCode;
import br.dev.andrestamatto.identityhub.identity.application.LocalIdentityVerificationException;
import br.dev.andrestamatto.identityhub.identity.application.LocalIdentityVerifier;
import br.dev.andrestamatto.identityhub.identity.application.PendingLocalIdentity;
import br.dev.andrestamatto.identityhub.identity.application.PasswordRecoveryIdentity;
import br.dev.andrestamatto.identityhub.identity.application.PasswordRecoveryIdentityFinder;
import br.dev.andrestamatto.identityhub.identity.application.PasswordRecoveryIdentityLookupException;
import br.dev.andrestamatto.identityhub.identity.domain.LoginEmail;
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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class KeycloakLocalIdentityRegistrar
        implements LocalIdentityRegistrar, LocalIdentityVerifier, PasswordRecoveryIdentityFinder {

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
            var existing = findUser(identity.email(), accessToken);
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

    @Override
    public Optional<PasswordRecoveryIdentity> findEligible(LoginEmail email) {
        Objects.requireNonNull(email);
        try {
            var accessToken = requestManagementToken();
            var user = findUser(email, accessToken);
            if (!isEligibleIdentity(user, email)) {
                return Optional.empty();
            }
            var userAccountRef = new UserAccountRef(
                    UUID.fromString(user.required("id").asString()));
            if (!hasPasswordCredential(userAccountRef, accessToken)) {
                return Optional.empty();
            }
            return Optional.of(new PasswordRecoveryIdentity(
                    userAccountRef,
                    new LoginEmail(user.required("email").asString())));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PasswordRecoveryIdentityLookupException(exception);
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof PasswordRecoveryIdentityLookupException lookupException) {
                throw lookupException;
            }
            throw new PasswordRecoveryIdentityLookupException(exception);
        }
    }

    @Override
    public void verifyAndEnable(UserAccountRef userAccountRef, LoginEmail expectedEmail) {
        Objects.requireNonNull(userAccountRef);
        Objects.requireNonNull(expectedEmail);
        try {
            var accessToken = requestManagementToken();
            var response = sendAuthorized(
                    HttpRequest.newBuilder(userUri(userAccountRef)).GET(), accessToken);
            ensureSuccess(
                    response.statusCode(),
                    LocalIdentityRegistrationFailureCode.MANAGEMENT_REQUEST_REJECTED);
            var user = objectMapper.readTree(response.body());
            if (user.path("email").isMissingNode()
                    || !new LoginEmail(user.path("email").asString()).equals(expectedEmail)) {
                throw new LocalIdentityVerificationException(false);
            }
            if (user.path("enabled").asBoolean()
                    && user.path("emailVerified").asBoolean()) {
                return;
            }
            var representation = new LinkedHashMap<String, Object>();
            representation.put("id", user.required("id").asString());
            representation.put("username", user.required("username").asString());
            if (!user.path("email").isMissingNode()) {
                representation.put("email", user.path("email").asString());
            }
            if (!user.path("attributes").isMissingNode()) {
                representation.put("attributes", user.path("attributes"));
            }
            representation.put("enabled", true);
            representation.put("emailVerified", true);
            var update = sendAuthorized(
                    HttpRequest.newBuilder(userUri(userAccountRef))
                            .header("Content-Type", "application/json")
                            .PUT(HttpRequest.BodyPublishers.ofByteArray(
                                    objectMapper.writeValueAsBytes(representation))),
                    accessToken);
            ensureSuccess(
                    update.statusCode(),
                    LocalIdentityRegistrationFailureCode.MANAGEMENT_REQUEST_REJECTED);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LocalIdentityVerificationException(true);
        } catch (IOException exception) {
            throw new LocalIdentityVerificationException(true);
        } catch (LocalIdentityRegistrationException exception) {
            throw new LocalIdentityVerificationException(exception.retryable());
        } catch (LocalIdentityVerificationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new LocalIdentityVerificationException(true);
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

    private JsonNode findUser(LoginEmail email, String accessToken)
            throws IOException, InterruptedException {
        var response = sendAuthorized(
                HttpRequest.newBuilder(userLookupUri(email)).GET(), accessToken);
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
            if (users.isEmpty()) {
                return null;
            }
            var userAccountRef = new UserAccountRef(
                    UUID.fromString(users.getFirst().required("id").asString()));
            var fullUser = sendAuthorized(
                    HttpRequest.newBuilder(userUri(userAccountRef)).GET(), accessToken);
            ensureSuccess(
                    fullUser.statusCode(),
                    LocalIdentityRegistrationFailureCode.MANAGEMENT_REQUEST_REJECTED);
            return objectMapper.readTree(fullUser.body());
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
                var racedUser = findUser(identity.email(), accessToken);
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

    private URI userLookupUri(LoginEmail email) {
        return URI.create(userCollectionUri() + "?username="
                + encode(email.normalizedValue()) + "&exact=true");
    }

    private boolean isEligibleIdentity(JsonNode user, LoginEmail expectedEmail) {
        if (user == null || !user.path("enabled").asBoolean()
                || !user.path("emailVerified").asBoolean()
                || user.path("email").isMissingNode()
                || !new LoginEmail(user.path("email").asString()).equals(expectedEmail)) {
            return false;
        }
        return true;
    }

    private boolean hasPasswordCredential(UserAccountRef userAccountRef, String accessToken)
            throws IOException, InterruptedException {
        var response = sendAuthorized(
                HttpRequest.newBuilder(credentialsUri(userAccountRef)).GET(), accessToken);
        ensureSuccess(
                response.statusCode(),
                LocalIdentityRegistrationFailureCode.MANAGEMENT_REQUEST_REJECTED);
        var credentials = objectMapper.readValue(
                response.body(), new TypeReference<List<JsonNode>>() { });
        return credentials.stream()
                .anyMatch(credential -> "password".equals(credential.path("type").asString()));
    }

    private URI credentialsUri(UserAccountRef userAccountRef) {
        return URI.create(userUri(userAccountRef) + "/credentials");
    }

    private URI userUri(UserAccountRef userAccountRef) {
        return URI.create(userCollectionUri() + "/" + userAccountRef.value());
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
