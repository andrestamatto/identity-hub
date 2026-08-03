package br.dev.andrestamatto.identityhub.access.adapter.out.keycloak;

import br.dev.andrestamatto.identityhub.access.application.MembershipProjectionException;
import br.dev.andrestamatto.identityhub.access.application.MembershipProjectionFailureCode;
import br.dev.andrestamatto.identityhub.access.application.MembershipProjector;
import br.dev.andrestamatto.identityhub.access.domain.Membership;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class KeycloakMembershipProjector implements MembershipProjector {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final String MANAGED_ATTRIBUTE = "identityhub.managed";
    private static final String APPLICATION_ATTRIBUTE = "identityhub.application-id";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI baseUri;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    public KeycloakMembershipProjector(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            URI baseUri,
            String realm,
            String clientId,
            String clientSecret) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.baseUri = Objects.requireNonNull(baseUri);
        this.realm = text(realm, "Realm");
        this.clientId = text(clientId, "Client id");
        this.clientSecret = text(clientSecret, "Client secret");
    }

    @Override
    public void project(Membership membership) {
        Objects.requireNonNull(membership);
        try {
            var accessToken = requestToken();
            ensureUserExists(membership, accessToken);
            var marker = ensureMarker(membership, accessToken);
            join(membership, marker.required("id").asString(), accessToken);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw retryable(MembershipProjectionFailureCode.KEYCLOAK_UNAVAILABLE, exception);
        } catch (IOException exception) {
            throw retryable(MembershipProjectionFailureCode.KEYCLOAK_UNAVAILABLE, exception);
        } catch (MembershipProjectionException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw retryable(MembershipProjectionFailureCode.KEYCLOAK_INVALID_RESPONSE, exception);
        }
    }

    private String requestToken() throws IOException, InterruptedException {
        var credentials = Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        var response = httpClient.send(HttpRequest.newBuilder(tokenUri())
                        .timeout(REQUEST_TIMEOUT)
                        .header("Authorization", "Basic " + credentials)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                        .build(), HttpResponse.BodyHandlers.ofString());
        ensureSuccess(response.statusCode());
        return objectMapper.readTree(response.body()).required("access_token").asString();
    }

    private void ensureUserExists(Membership membership, String accessToken)
            throws IOException, InterruptedException {
        var response = send(HttpRequest.newBuilder(userUri(membership)).GET(), accessToken);
        if (response.statusCode() == 404) {
            throw MembershipProjectionException.permanent(
                    MembershipProjectionFailureCode.USER_NOT_FOUND, null);
        }
        ensureSuccess(response.statusCode());
    }

    private JsonNode ensureMarker(Membership membership, String accessToken)
            throws IOException, InterruptedException {
        var existing = findMarker(membership, accessToken);
        if (existing == null) {
            var response = send(HttpRequest.newBuilder(groupCollectionUri())
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofByteArray(
                                    objectMapper.writeValueAsBytes(representation(membership)))),
                    accessToken);
            if (response.statusCode() != 409) {
                ensureSuccess(response.statusCode());
            }
            existing = findMarker(membership, accessToken);
            if (existing == null) {
                throw retryable(MembershipProjectionFailureCode.KEYCLOAK_INVALID_RESPONSE, null);
            }
        }
        ensureOwnership(existing, membership);
        return existing;
    }

    private JsonNode findMarker(Membership membership, String accessToken)
            throws IOException, InterruptedException {
        var response = send(HttpRequest.newBuilder(groupLookupUri(membership)).GET(), accessToken);
        ensureSuccess(response.statusCode());
        var groups = objectMapper.readValue(
                response.body(), new TypeReference<List<JsonNode>>() { }).stream()
                .filter(group -> markerName(membership).equals(group.path("name").asString()))
                .toList();
        if (groups.size() > 1) {
            throw MembershipProjectionException.permanent(
                    MembershipProjectionFailureCode.MARKER_CONFLICT, null);
        }
        return groups.isEmpty() ? null : groups.getFirst();
    }

    private void ensureOwnership(JsonNode marker, Membership membership) {
        var attributes = marker.path("attributes");
        if (!contains(attributes.path(MANAGED_ATTRIBUTE), "true")
                || !contains(attributes.path(APPLICATION_ATTRIBUTE),
                        membership.applicationRef().value().toString())) {
            throw MembershipProjectionException.permanent(
                    MembershipProjectionFailureCode.MARKER_CONFLICT, null);
        }
    }

    private void join(Membership membership, String markerId, String accessToken)
            throws IOException, InterruptedException {
        var response = send(HttpRequest.newBuilder(URI.create(
                                userUri(membership) + "/groups/" + encode(markerId)))
                        .PUT(HttpRequest.BodyPublishers.noBody()),
                accessToken);
        if (response.statusCode() == 404) {
            throw retryable(MembershipProjectionFailureCode.KEYCLOAK_INVALID_RESPONSE, null);
        }
        ensureSuccess(response.statusCode());
    }

    private Map<String, Object> representation(Membership membership) {
        return Map.of(
                "name", markerName(membership),
                "attributes", Map.of(
                        MANAGED_ATTRIBUTE, List.of("true"),
                        APPLICATION_ATTRIBUTE,
                        List.of(membership.applicationRef().value().toString())));
    }

    private HttpResponse<String> send(HttpRequest.Builder request, String accessToken)
            throws IOException, InterruptedException {
        return httpClient.send(request.timeout(REQUEST_TIMEOUT)
                        .header("Authorization", "Bearer " + accessToken)
                        .build(), HttpResponse.BodyHandlers.ofString());
    }

    private void ensureSuccess(int status) {
        if (status >= 200 && status < 300) {
            return;
        }
        if (status >= 500) {
            throw retryable(MembershipProjectionFailureCode.KEYCLOAK_UNAVAILABLE, null);
        }
        throw MembershipProjectionException.permanent(
                MembershipProjectionFailureCode.KEYCLOAK_REJECTED, null);
    }

    private URI tokenUri() {
        return baseUri.resolve("/realms/" + encode(realm) + "/protocol/openid-connect/token");
    }

    private URI groupCollectionUri() {
        return baseUri.resolve("/admin/realms/" + encode(realm) + "/groups");
    }

    private URI groupLookupUri(Membership membership) {
        return URI.create(groupCollectionUri() + "?search=" + encode(markerName(membership))
                + "&exact=true&briefRepresentation=false");
    }

    private URI userUri(Membership membership) {
        return baseUri.resolve("/admin/realms/" + encode(realm) + "/users/"
                + membership.userAccountRef().value());
    }

    private String markerName(Membership membership) {
        return "ih-membership-" + membership.applicationRef().value();
    }

    private static boolean contains(JsonNode values, String expected) {
        for (var value : values) {
            if (expected.equals(value.asString())) {
                return true;
            }
        }
        return false;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String text(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private static MembershipProjectionException retryable(
            MembershipProjectionFailureCode code, Throwable cause) {
        return MembershipProjectionException.retryable(code, cause);
    }
}
