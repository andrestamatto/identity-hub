package br.dev.andrestamatto.identityhub.clientapplication.adapter.out.keycloak;

import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientProjectionException;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientProjectionFailureCode;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientProjector;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientSnapshot;
import br.dev.andrestamatto.identityhub.clientapplication.application.ConfidentialClientSecretRotator;
import br.dev.andrestamatto.identityhub.clientapplication.application.ConfidentialClientSecret;
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

public final class KeycloakApplicationClientProjector
        implements ApplicationClientProjector, ConfidentialClientSecretRotator {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    static final String MANAGED_ATTRIBUTE = "identityhub.managed";
    static final String CLIENT_ID_ATTRIBUTE = "identityhub.application-client-id";
    static final String CLIENT_TYPE_ATTRIBUTE = "identityhub.application-client-type";
    static final String AUDIENCE_ATTRIBUTE = "identityhub.audience";
    static final String PKCE_METHOD_ATTRIBUTE = "pkce.code.challenge.method";
    static final String MEMBERSHIP_WRITE_SCOPE = "membership:write";
    static final String INTEGRATION_AUDIENCE = "identityhub-integration-api";
    private static final String CLIENT_SCOPE_ATTRIBUTE = "identityhub.client-scope";

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
            JsonNode membershipScope = null;
            if (client.type().equals("MACHINE")) {
                membershipScope = findMembershipScope(accessToken);
                if (client.scopes().contains(MEMBERSHIP_WRITE_SCOPE)) {
                    membershipScope = ensureMembershipScope(membershipScope, accessToken);
                }
            }
            var existing = findClient(client, accessToken);
            if (existing == null) {
                createClient(client, accessToken);
                existing = findClient(client, accessToken);
                if (existing == null) {
                    throw ApplicationClientProjectionException.retryable(
                            ApplicationClientProjectionFailureCode.KEYCLOAK_INVALID_RESPONSE,
                            null);
                }
            }
            ensureOwnership(existing, client);
            if (!matches(existing, client)) {
                updateClient(existing.required("id").asString(), client, accessToken);
            }
            if (client.type().equals("MACHINE")) {
                reconcileMachineScopes(
                        existing.required("id").asString(),
                        membershipScope,
                        client.scopes().contains(MEMBERSHIP_WRITE_SCOPE),
                        accessToken);
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

    @Override
    public ConfidentialClientSecret rotate(ApplicationClientSnapshot client) {
        Objects.requireNonNull(client);
        if (!(client.type().equals("BFF") || client.type().equals("MACHINE"))) {
            throw new IllegalArgumentException("Only confidential clients have a rotatable secret");
        }
        try {
            var accessToken = requestManagementToken();
            var existing = findClient(client, accessToken);
            if (existing == null) {
                throw ApplicationClientProjectionException.permanent(
                        ApplicationClientProjectionFailureCode.KEYCLOAK_CLIENT_CONFLICT, null);
            }
            ensureOwnership(existing, client);
            var keycloakId = existing.required("id").asString();
            var response = sendAuthorized(
                    HttpRequest.newBuilder(clientCollectionUri().resolve(
                                    keycloakId + "/client-secret"))
                            .POST(HttpRequest.BodyPublishers.noBody()),
                    accessToken);
            ensureSuccess(
                    response.statusCode(),
                    ApplicationClientProjectionFailureCode.KEYCLOAK_MANAGEMENT_REJECTED);
            return new ConfidentialClientSecret(
                    objectMapper.readTree(response.body()).required("value").asString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw ApplicationClientProjectionException.retryable(
                    ApplicationClientProjectionFailureCode.KEYCLOAK_UNAVAILABLE, exception);
        } catch (IOException exception) {
            throw ApplicationClientProjectionException.retryable(
                    ApplicationClientProjectionFailureCode.KEYCLOAK_UNAVAILABLE, exception);
        } catch (ApplicationClientProjectionException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw ApplicationClientProjectionException.retryable(
                    ApplicationClientProjectionFailureCode.KEYCLOAK_INVALID_RESPONSE, exception);
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

    private JsonNode findMembershipScope(String accessToken)
            throws IOException, InterruptedException {
        var response = sendAuthorized(
                HttpRequest.newBuilder(clientScopeCollectionUri()).GET(),
                accessToken);
        ensureSuccess(
                response.statusCode(),
                ApplicationClientProjectionFailureCode.KEYCLOAK_MANAGEMENT_REJECTED);
        try {
            var scopes = objectMapper.readValue(
                    response.body(),
                    new TypeReference<List<JsonNode>>() { }).stream()
                    .filter(scope -> MEMBERSHIP_WRITE_SCOPE.equals(scope.path("name").asString()))
                    .toList();
            if (scopes.size() > 1) {
                throw ApplicationClientProjectionException.permanent(
                        ApplicationClientProjectionFailureCode.KEYCLOAK_CLIENT_CONFLICT, null);
            }
            if (scopes.isEmpty()) {
                return null;
            }
            var scopeId = scopes.getFirst().required("id").asString();
            var detail = sendAuthorized(
                    HttpRequest.newBuilder(clientScopeUri(scopeId)).GET(),
                    accessToken);
            ensureSuccess(
                    detail.statusCode(),
                    ApplicationClientProjectionFailureCode.KEYCLOAK_MANAGEMENT_REJECTED);
            return objectMapper.readTree(detail.body());
        } catch (ApplicationClientProjectionException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw ApplicationClientProjectionException.retryable(
                    ApplicationClientProjectionFailureCode.KEYCLOAK_INVALID_RESPONSE,
                    exception);
        }
    }

    private JsonNode ensureMembershipScope(JsonNode existing, String accessToken)
            throws IOException, InterruptedException {
        if (existing == null) {
            var response = sendAuthorized(
                    HttpRequest.newBuilder(clientScopeCollectionUri())
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(
                                    membershipScopeRepresentation(null))),
                    accessToken);
            if (response.statusCode() != 409) {
                ensureSuccess(
                        response.statusCode(),
                        ApplicationClientProjectionFailureCode.KEYCLOAK_MANAGEMENT_REJECTED);
            }
            existing = findMembershipScope(accessToken);
            if (existing == null) {
                throw ApplicationClientProjectionException.retryable(
                        ApplicationClientProjectionFailureCode.KEYCLOAK_INVALID_RESPONSE,
                        null);
            }
        }
        ensureMembershipScopeOwnership(existing);
        if (!membershipScopeMatches(existing)) {
            var scopeId = existing.required("id").asString();
            var response = sendAuthorized(
                    HttpRequest.newBuilder(clientScopeUri(scopeId))
                            .header("Content-Type", "application/json")
                            .PUT(HttpRequest.BodyPublishers.ofString(
                                    membershipScopeRepresentation(scopeId))),
                    accessToken);
            ensureSuccess(
                    response.statusCode(),
                    ApplicationClientProjectionFailureCode.KEYCLOAK_MANAGEMENT_REJECTED);
            return objectMapper.readTree(membershipScopeRepresentation(scopeId));
        }
        return existing;
    }

    private void reconcileMachineScopes(
            String keycloakClientId,
            JsonNode membershipScope,
            boolean desired,
            String accessToken) throws IOException, InterruptedException {
        var response = sendAuthorized(
                HttpRequest.newBuilder(defaultClientScopesUri(keycloakClientId)).GET(),
                accessToken);
        ensureSuccess(
                response.statusCode(),
                ApplicationClientProjectionFailureCode.KEYCLOAK_MANAGEMENT_REJECTED);
        var attachedScopes = objectMapper.readValue(
                response.body(), new TypeReference<List<JsonNode>>() { });
        var membershipScopeId = membershipScope == null
                ? null
                : membershipScope.required("id").asString();
        var membershipAttached = false;
        for (var attachedScope : attachedScopes) {
            var attachedScopeId = attachedScope.required("id").asString();
            if (desired && attachedScopeId.equals(membershipScopeId)) {
                membershipAttached = true;
                continue;
            }
            var removal = sendAuthorized(
                    HttpRequest.newBuilder(
                                    defaultClientScopeUri(keycloakClientId, attachedScopeId))
                            .DELETE(),
                    accessToken);
            ensureSuccess(
                    removal.statusCode(),
                    ApplicationClientProjectionFailureCode.KEYCLOAK_MANAGEMENT_REJECTED);
        }
        if (desired && !membershipAttached) {
            var attachment = sendAuthorized(
                    HttpRequest.newBuilder(
                                    defaultClientScopeUri(keycloakClientId, membershipScopeId))
                            .PUT(HttpRequest.BodyPublishers.noBody()),
                    accessToken);
            ensureSuccess(
                    attachment.statusCode(),
                    ApplicationClientProjectionFailureCode.KEYCLOAK_MANAGEMENT_REJECTED);
        }
    }

    private void ensureMembershipScopeOwnership(JsonNode scope) {
        if (!"true".equals(scope.path("attributes").path(MANAGED_ATTRIBUTE).asString())
                || !MEMBERSHIP_WRITE_SCOPE.equals(
                        scope.path("attributes").path(CLIENT_SCOPE_ATTRIBUTE).asString())) {
            throw ApplicationClientProjectionException.permanent(
                    ApplicationClientProjectionFailureCode.KEYCLOAK_CLIENT_CONFLICT, null);
        }
    }

    private boolean membershipScopeMatches(JsonNode scope) {
        var attributes = scope.path("attributes");
        var mapperMatches = false;
        for (var mapper : scope.path("protocolMappers")) {
            if ("oidc-audience-mapper".equals(mapper.path("protocolMapper").asString())
                    && INTEGRATION_AUDIENCE.equals(mapper.path("config")
                            .path("included.custom.audience").asString())
                    && "true".equals(mapper.path("config")
                            .path("access.token.claim").asString())) {
                mapperMatches = true;
            }
        }
        return MEMBERSHIP_WRITE_SCOPE.equals(scope.path("name").asString())
                && "openid-connect".equals(scope.path("protocol").asString())
                && "true".equals(attributes.path("include.in.token.scope").asString())
                && mapperMatches;
    }

    private String membershipScopeRepresentation(String scopeId) {
        var representation = new java.util.LinkedHashMap<String, Object>();
        if (scopeId != null) {
            representation.put("id", scopeId);
        }
        representation.put("name", MEMBERSHIP_WRITE_SCOPE);
        representation.put("description", "IdentityHub membership provisioning permission");
        representation.put("protocol", "openid-connect");
        representation.put("attributes", Map.of(
                MANAGED_ATTRIBUTE, "true",
                CLIENT_SCOPE_ATTRIBUTE, MEMBERSHIP_WRITE_SCOPE,
                "include.in.token.scope", "true",
                "display.on.consent.screen", "false"));
        representation.put("protocolMappers", List.of(Map.of(
                "name", "identityhub-integration-audience",
                "protocol", "openid-connect",
                "protocolMapper", "oidc-audience-mapper",
                "consentRequired", false,
                "config", Map.of(
                        "included.custom.audience", INTEGRATION_AUDIENCE,
                        "access.token.claim", "true",
                        "id.token.claim", "false",
                        "introspection.token.claim", "true"))));
        return objectMapper.writeValueAsString(representation);
    }

    private HttpResponse<String> sendAuthorized(
            HttpRequest.Builder request,
            String accessToken) throws IOException, InterruptedException {
        return httpClient.send(
                request.timeout(REQUEST_TIMEOUT)
                        .header("Authorization", "Bearer " + accessToken)
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private String representation(ApplicationClientSnapshot client, String keycloakId) {
        var representation = new java.util.LinkedHashMap<String, Object>();
        if (keycloakId != null) {
            representation.put("id", keycloakId);
        }
        representation.put("clientId", keycloakClientId(client));
        var spa = client.type().equals("SPA");
        var bff = client.type().equals("BFF");
        var machine = client.type().equals("MACHINE");
        var browserClient = spa || bff;
        representation.put("name", "IdentityHub " + client.type() + " " + client.key());
        representation.put("description", "Managed " + client.type() + " projection");
        representation.put("protocol", "openid-connect");
        representation.put("enabled", client.enabled());
        representation.put("bearerOnly", client.type().equals("API"));
        representation.put("publicClient", spa);
        if (bff || machine) {
            representation.put("clientAuthenticatorType", "client-secret");
        }
        representation.put("standardFlowEnabled", browserClient);
        representation.put("implicitFlowEnabled", false);
        representation.put("directAccessGrantsEnabled", false);
        representation.put("serviceAccountsEnabled", machine);
        representation.put("authorizationServicesEnabled", false);
        representation.put("fullScopeAllowed", false);
        representation.put("redirectUris", client.redirectUris());
        representation.put("webOrigins", client.webOrigins());
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
        var browserClient = client.type().equals("SPA") || client.type().equals("BFF");
        var confidentialClient = client.type().equals("BFF") || client.type().equals("MACHINE");
        return existing.path("enabled").asBoolean() == client.enabled()
                && existing.path("bearerOnly").asBoolean() == client.type().equals("API")
                && existing.path("publicClient").asBoolean() == client.type().equals("SPA")
                && existing.path("standardFlowEnabled").asBoolean() == browserClient
                && (!confidentialClient
                        || existing.path("clientAuthenticatorType").asString()
                                .equals("client-secret"))
                && !existing.path("implicitFlowEnabled").asBoolean()
                && !existing.path("directAccessGrantsEnabled").asBoolean()
                && existing.path("serviceAccountsEnabled").asBoolean()
                        == client.type().equals("MACHINE")
                && !existing.path("authorizationServicesEnabled").asBoolean()
                && !existing.path("fullScopeAllowed").asBoolean()
                && client.type().equals(attributes.path(CLIENT_TYPE_ATTRIBUTE).asString())
                && typeSpecificSettingsMatch(existing, attributes, client);
    }

    private Map<String, String> managedAttributes(ApplicationClientSnapshot client) {
        var attributes = new java.util.LinkedHashMap<String, String>();
        attributes.put(MANAGED_ATTRIBUTE, "true");
        attributes.put(CLIENT_ID_ATTRIBUTE, client.applicationClientId().toString());
        attributes.put(CLIENT_TYPE_ATTRIBUTE, client.type());
        if (client.type().equals("API")) {
            attributes.put(AUDIENCE_ATTRIBUTE, client.audience());
        } else if (!client.type().equals("MACHINE")) {
            attributes.put(PKCE_METHOD_ATTRIBUTE, "S256");
        }
        return Map.copyOf(attributes);
    }

    private boolean typeSpecificSettingsMatch(
            JsonNode existing,
            JsonNode attributes,
            ApplicationClientSnapshot client) {
        if (client.type().equals("API")) {
            return client.audience().equals(attributes.path(AUDIENCE_ATTRIBUTE).asString())
                    && existing.path("redirectUris").isEmpty()
                    && existing.path("webOrigins").isEmpty();
        }
        if (client.type().equals("MACHINE")) {
            return existing.path("redirectUris").isEmpty()
                    && existing.path("webOrigins").isEmpty();
        }
        return "S256".equals(attributes.path(PKCE_METHOD_ATTRIBUTE).asString())
                && jsonStrings(existing.path("redirectUris")).equals(client.redirectUris())
                && jsonStrings(existing.path("webOrigins")).equals(client.webOrigins());
    }

    private List<String> jsonStrings(JsonNode array) {
        var values = new java.util.ArrayList<String>();
        array.forEach(node -> values.add(node.asString()));
        return List.copyOf(values);
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

    private URI clientScopeCollectionUri() {
        return baseUri.resolve("/admin/realms/" + encode(realm) + "/client-scopes");
    }

    private URI clientScopeUri(String scopeId) {
        return URI.create(clientScopeCollectionUri() + "/" + encode(scopeId));
    }

    private URI defaultClientScopesUri(String keycloakClientId) {
        return clientCollectionUri().resolve(
                encode(keycloakClientId) + "/default-client-scopes");
    }

    private URI defaultClientScopeUri(String keycloakClientId, String scopeId) {
        return URI.create(defaultClientScopesUri(keycloakClientId) + "/" + encode(scopeId));
    }

    private String keycloakClientId(ApplicationClientSnapshot client) {
        return "ih-" + client.type().toLowerCase(java.util.Locale.ROOT)
                + "-" + client.applicationClientId();
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
