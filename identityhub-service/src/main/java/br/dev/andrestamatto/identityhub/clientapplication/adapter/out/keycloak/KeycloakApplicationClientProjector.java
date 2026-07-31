package br.dev.andrestamatto.identityhub.clientapplication.adapter.out.keycloak;

import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientProjectionException;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientProjectionFailureCode;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientProjector;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientSnapshot;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class KeycloakApplicationClientProjector implements ApplicationClientProjector {

    static final String MANAGED_ATTRIBUTE = "identityhub.managed";
    static final String CLIENT_ID_ATTRIBUTE = "identityhub.application-client-id";
    static final String AUDIENCE_ATTRIBUTE = "identityhub.audience";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI baseUri;
    private final String realm;
    private final String managementClientId;
    private final String managementClientSecret;

    public KeycloakApplicationClientProjector(
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
                managementClientSecret,
                "Management client secret");
    }

    @Override
    public void project(ApplicationClientSnapshot client) {
        Objects.requireNonNull(client);
        try {
            var accessToken = requestManagementToken();
            var existing = findClient(client, accessToken);
            if (existing == null) {
                createClient(client, accessToken);
                return;
            }
            ensureOwnership(existing, client);
            if (!matches(existing, client)) {
                updateClient(existing.required("id").asString(), client, accessToken);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw ApplicationClientProjectionException.retryable(
                    ApplicationClientProjectionFailureCode.KEYCLOAK_UNAVAILABLE, exception);
        } catch (IOException exception) {
            throw ApplicationClientProjectionException.retryable(
                    ApplicationClientProjectionFailureCode.KEYCLOAK_UNAVAILABLE, exception);
        }
    }

    private String requestManagementToken() throws IOException, InterruptedException {
        var credentials = Base64.getEncoder().encodeToString(
                (managementClientId + ":" + managementClientSecret)
                        .getBytes(StandardCharsets.UTF_8));
        var request = HttpRequest.newBuilder(tokenUri())
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        ensureSuccess(
                response.statusCode(),
                ApplicationClientProjectionFailureCode.KEYCLOAK_MANAGEMENT_AUTH_REJECTED);
        try {
            return objectMapper.readTree(response.body()).required("access_token").asString();
        } catch (RuntimeException exception) {
            throw ApplicationClientProjectionException.retryable(
                    ApplicationClientProjectionFailureCode.KEYCLOAK_INVALID_RESPONSE, exception);
        }
    }

    private JsonNode findClient(ApplicationClientSnapshot client, String accessToken)
            throws IOException, InterruptedException {
        var response = sendAuthorized(
                HttpRequest.newBuilder(clientCollectionUri(client))
                        .GET(),
                accessToken);
        ensureSuccess(
                response.statusCode(),
                ApplicationClientProjectionFailureCode.KEYCLOAK_MANAGEMENT_REJECTED);
        try {
            var clients = objectMapper.readValue(
                    response.body(),
                    new TypeReference<List<JsonNode>>() { });
            if (clients.size() > 1) {
                throw ApplicationClientProjectionException.permanent(
                        ApplicationClientProjectionFailureCode.KEYCLOAK_CLIENT_CONFLICT, null);
            }
            return clients.isEmpty() ? null : clients.getFirst();
        } catch (ApplicationClientProjectionException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw ApplicationClientProjectionException.retryable(
                    ApplicationClientProjectionFailureCode.KEYCLOAK_INVALID_RESPONSE, exception);
        }
    }

    private void createClient(ApplicationClientSnapshot client, String accessToken)
            throws IOException, InterruptedException {
        var response = sendAuthorized(
                HttpRequest.newBuilder(clientCollectionUri())
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(representation(client, null))),
                accessToken);
        if (response.statusCode() == 409) {
            var racedClient = findClient(client, accessToken);
            if (racedClient != null) {
                ensureOwnership(racedClient, client);
                return;
            }
        }
        ensureSuccess(
                response.statusCode(),
                ApplicationClientProjectionFailureCode.KEYCLOAK_MANAGEMENT_REJECTED);
    }

    private void updateClient(
            String keycloakId,
            ApplicationClientSnapshot client,
            String accessToken) throws IOException, InterruptedException {
        var response = sendAuthorized(
                HttpRequest.newBuilder(clientCollectionUri().resolve(keycloakId))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(
                                representation(client, keycloakId))),
                accessToken);
        ensureSuccess(
                response.statusCode(),
                ApplicationClientProjectionFailureCode.KEYCLOAK_MANAGEMENT_REJECTED);
    }

    private HttpResponse<String> sendAuthorized(
            HttpRequest.Builder request,
            String accessToken) throws IOException, InterruptedException {
        return httpClient.send(
                request.header("Authorization", "Bearer " + accessToken).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private String representation(ApplicationClientSnapshot client, String keycloakId) {
        var representation = new java.util.LinkedHashMap<String, Object>();
        if (keycloakId != null) {
            representation.put("id", keycloakId);
        }
        representation.put("clientId", keycloakClientId(client));
        representation.put("name", "IdentityHub API " + client.key());
        representation.put("description", "Managed protected API projection");
        representation.put("protocol", "openid-connect");
        representation.put("enabled", client.enabled());
        representation.put("bearerOnly", true);
        representation.put("publicClient", false);
        representation.put("standardFlowEnabled", false);
        representation.put("implicitFlowEnabled", false);
        representation.put("directAccessGrantsEnabled", false);
        representation.put("serviceAccountsEnabled", false);
        representation.put("authorizationServicesEnabled", false);
        representation.put("fullScopeAllowed", false);
        representation.put("redirectUris", List.of());
        representation.put("webOrigins", List.of());
        representation.put("attributes", managedAttributes(client));
        return objectMapper.writeValueAsString(representation);
    }

    private void ensureOwnership(JsonNode existing, ApplicationClientSnapshot client) {
        var attributes = existing.path("attributes");
        if (!"true".equals(attributes.path(MANAGED_ATTRIBUTE).asString())
                || !client.applicationClientId().toString()
                        .equals(attributes.path(CLIENT_ID_ATTRIBUTE).asString())) {
            throw ApplicationClientProjectionException.permanent(
                    ApplicationClientProjectionFailureCode.KEYCLOAK_CLIENT_CONFLICT, null);
        }
    }

    private boolean matches(JsonNode existing, ApplicationClientSnapshot client) {
        var attributes = existing.path("attributes");
        return existing.path("enabled").asBoolean() == client.enabled()
                && existing.path("bearerOnly").asBoolean()
                && !existing.path("standardFlowEnabled").asBoolean()
                && !existing.path("implicitFlowEnabled").asBoolean()
                && !existing.path("directAccessGrantsEnabled").asBoolean()
                && !existing.path("serviceAccountsEnabled").asBoolean()
                && !existing.path("authorizationServicesEnabled").asBoolean()
                && !existing.path("fullScopeAllowed").asBoolean()
                && client.audience().equals(attributes.path(AUDIENCE_ATTRIBUTE).asString());
    }

    private Map<String, String> managedAttributes(ApplicationClientSnapshot client) {
        return Map.of(
                MANAGED_ATTRIBUTE, "true",
                CLIENT_ID_ATTRIBUTE, client.applicationClientId().toString(),
                AUDIENCE_ATTRIBUTE, client.audience());
    }

    private URI tokenUri() {
        return baseUri.resolve("/realms/" + encode(realm)
                + "/protocol/openid-connect/token");
    }

    private URI clientCollectionUri() {
        return baseUri.resolve("/admin/realms/" + encode(realm) + "/clients/");
    }

    private URI clientCollectionUri(ApplicationClientSnapshot client) {
        return URI.create(clientCollectionUri().toString()
                + "?clientId=" + encode(keycloakClientId(client))
                + "&exact=true");
    }

    private String keycloakClientId(ApplicationClientSnapshot client) {
        return "ih-api-" + client.applicationClientId();
    }

    private static void ensureSuccess(
            int statusCode,
            ApplicationClientProjectionFailureCode permanentFailureCode) {
        if (statusCode >= 200 && statusCode < 300) {
            return;
        }
        if (statusCode == 401 || statusCode == 403 || statusCode == 400) {
            throw ApplicationClientProjectionException.permanent(
                    permanentFailureCode,
                    new IllegalStateException("Keycloak rejected management request"));
        }
        throw ApplicationClientProjectionException.retryable(
                ApplicationClientProjectionFailureCode.KEYCLOAK_UNAVAILABLE,
                new IllegalStateException("Keycloak management request failed"));
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
