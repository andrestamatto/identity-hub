package br.dev.andrestamatto.identityhub.access.adapter.out.keycloak;

import br.dev.andrestamatto.identityhub.access.application.MembershipProjectionException;
import br.dev.andrestamatto.identityhub.access.application.MembershipProjectionFailureCode;
import br.dev.andrestamatto.identityhub.access.domain.Membership;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationTokenClient;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationTokenClientResolver;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class KeycloakMembershipTokenProjector {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final String MANAGED_ATTRIBUTE = "identityhub.managed";
    private static final String APPLICATION_ATTRIBUTE = "identityhub.application-id";
    private static final String CLIENT_ID_ATTRIBUTE = "identityhub.application-client-id";
    private static final String CLIENT_TYPE_ATTRIBUTE = "identityhub.application-client-type";
    private static final String ACCESS_ROLE = "ih-membership-access";
    private static final String KEYCLOAK_BASIC_SCOPE = "basic";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI baseUri;
    private final String realm;
    private final String clientId;
    private final String clientSecret;
    private final ApplicationTokenClientResolver clientResolver;

    public KeycloakMembershipTokenProjector(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            URI baseUri,
            String realm,
            String clientId,
            String clientSecret,
            ApplicationTokenClientResolver clientResolver) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.baseUri = Objects.requireNonNull(baseUri);
        this.realm = text(realm, "Realm");
        this.clientId = text(clientId, "Client id");
        this.clientSecret = text(clientSecret, "Client secret");
        this.clientResolver = Objects.requireNonNull(clientResolver);
    }

    public List<KeycloakClientRole> project(Membership membership) {
        Objects.requireNonNull(membership);
        var clients = clientResolver.resolve(membership.applicationRef().value());
        if (clients.isEmpty()) {
            return List.of();
        }
        try {
            var accessToken = requestToken();
            var resolvedClients = resolveClients(clients, accessToken);
            var roles = ensureApiRoles(membership, resolvedClients, accessToken);
            var scope = ensureAccessScope(membership, accessToken);
            mapScopeRoles(scope, roles, accessToken);
            reconcileBrowserScopes(resolvedClients, scope, accessToken);
            return roles;
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

    private List<ResolvedClient> resolveClients(
            List<ApplicationTokenClient> clients,
            String accessToken) throws IOException, InterruptedException {
        var resolved = new ArrayList<ResolvedClient>();
        for (var client : clients) {
            var response = send(HttpRequest.newBuilder(URI.create(clientCollectionUri()
                            + "?clientId=" + encode(keycloakClientId(client)) + "&exact=true"))
                    .GET(), accessToken);
            ensureSuccess(response.statusCode());
            var matches = nodes(response.body());
            if (matches.size() != 1) {
                throw conflict();
            }
            var representation = matches.getFirst();
            ensureClientOwnership(representation, client);
            resolved.add(new ResolvedClient(client, representation.required("id").asString()));
        }
        return List.copyOf(resolved);
    }

    private void ensureClientOwnership(JsonNode representation, ApplicationTokenClient client) {
        var attributes = representation.path("attributes");
        if (!"true".equals(attributes.path(MANAGED_ATTRIBUTE).asString())
                || !client.id().toString().equals(attributes.path(CLIENT_ID_ATTRIBUTE).asString())
                || !client.type().equals(attributes.path(CLIENT_TYPE_ATTRIBUTE).asString())
                || representation.path("fullScopeAllowed").asBoolean()) {
            throw conflict();
        }
    }

    private List<KeycloakClientRole> ensureApiRoles(
            Membership membership,
            List<ResolvedClient> clients,
            String accessToken) throws IOException, InterruptedException {
        var roles = new ArrayList<KeycloakClientRole>();
        for (var client : clients.stream().filter(ResolvedClient::isApi).toList()) {
            var role = findRole(client, accessToken);
            if (role == null) {
                var response = send(HttpRequest.newBuilder(roleCollectionUri(client))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofByteArray(
                                        objectMapper.writeValueAsBytes(
                                                roleRepresentation(membership))))
                        , accessToken);
                if (response.statusCode() != 409) {
                    ensureSuccess(response.statusCode());
                }
                role = findRole(client, accessToken);
            }
            if (role == null) {
                throw retryable(MembershipProjectionFailureCode.KEYCLOAK_INVALID_RESPONSE, null);
            }
            ensureArrayOwnership(role.path("attributes"), membership.applicationRef().value());
            roles.add(new KeycloakClientRole(
                    client.internalId(),
                    role.required("id").asString(),
                    role.required("name").asString()));
        }
        return List.copyOf(roles);
    }

    private JsonNode findRole(ResolvedClient client, String accessToken)
            throws IOException, InterruptedException {
        var response = send(HttpRequest.newBuilder(roleUri(client)).GET(), accessToken);
        if (response.statusCode() == 404) {
            return null;
        }
        ensureSuccess(response.statusCode());
        return objectMapper.readTree(response.body());
    }

    private JsonNode ensureAccessScope(Membership membership, String accessToken)
            throws IOException, InterruptedException {
        var scope = findScope(membership, accessToken);
        if (scope == null) {
            var response = send(HttpRequest.newBuilder(clientScopeCollectionUri())
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofByteArray(
                                    objectMapper.writeValueAsBytes(
                                            scopeRepresentation(membership, null))))
                    , accessToken);
            if (response.statusCode() != 409) {
                ensureSuccess(response.statusCode());
            }
            scope = findScope(membership, accessToken);
        }
        if (scope == null) {
            throw retryable(MembershipProjectionFailureCode.KEYCLOAK_INVALID_RESPONSE, null);
        }
        ensureScopeOwnership(scope, membership);
        if (!scopeMatches(scope)) {
            var scopeId = scope.required("id").asString();
            var representation = scopeRepresentation(membership, scopeId);
            var response = send(HttpRequest.newBuilder(clientScopeUri(scopeId))
                            .header("Content-Type", "application/json")
                            .PUT(HttpRequest.BodyPublishers.ofByteArray(
                                    objectMapper.writeValueAsBytes(representation)))
                    , accessToken);
            ensureSuccess(response.statusCode());
            scope = objectMapper.valueToTree(representation);
        }
        return scope;
    }

    private JsonNode findScope(Membership membership, String accessToken)
            throws IOException, InterruptedException {
        var response = send(HttpRequest.newBuilder(clientScopeCollectionUri()).GET(), accessToken);
        ensureSuccess(response.statusCode());
        var name = scopeName(membership);
        var scopes = nodes(response.body()).stream()
                .filter(scope -> name.equals(scope.path("name").asString()))
                .toList();
        if (scopes.size() > 1) {
            throw conflict();
        }
        if (scopes.isEmpty()) {
            return null;
        }
        var detail = send(HttpRequest.newBuilder(clientScopeUri(
                        scopes.getFirst().required("id").asString())).GET(), accessToken);
        ensureSuccess(detail.statusCode());
        return objectMapper.readTree(detail.body());
    }

    private void ensureScopeOwnership(JsonNode scope, Membership membership) {
        var attributes = scope.path("attributes");
        if (!"true".equals(attributes.path(MANAGED_ATTRIBUTE).asString())
                || !membership.applicationRef().value().toString().equals(
                        attributes.path(APPLICATION_ATTRIBUTE).asString())) {
            throw conflict();
        }
    }

    private boolean scopeMatches(JsonNode scope) {
        return "openid-connect".equals(scope.path("protocol").asString())
                && "false".equals(scope.path("attributes")
                        .path("include.in.token.scope").asString())
                && hasOnlyExpectedMappers(scope.path("protocolMappers"));
    }

    private boolean hasOnlyExpectedMappers(JsonNode mappers) {
        if (!mappers.isArray() || mappers.size() != 2) {
            return false;
        }
        var audienceMapper = false;
        var rolesMapper = false;
        for (var mapper : mappers) {
            if (isAudienceResolveMapper(mapper)) {
                audienceMapper = true;
            } else if (isPublicRolesMapper(mapper)) {
                rolesMapper = true;
            } else {
                return false;
            }
        }
        return audienceMapper && rolesMapper;
    }

    private boolean isAudienceResolveMapper(JsonNode mapper) {
        var config = mapper.path("config");
        return "oidc-audience-resolve-mapper".equals(mapper.path("protocolMapper").asString())
                && "true".equals(config.path("access.token.claim").asString())
                && "false".equals(config.path("id.token.claim").asString())
                && "true".equals(config.path("introspection.token.claim").asString());
    }

    private boolean isPublicRolesMapper(JsonNode mapper) {
        var config = mapper.path("config");
        return "oidc-hardcoded-claim-mapper".equals(mapper.path("protocolMapper").asString())
                && "roles".equals(config.path("claim.name").asString())
                && "[]".equals(config.path("claim.value").asString())
                && "JSON".equals(config.path("jsonType.label").asString())
                && "true".equals(config.path("access.token.claim").asString())
                && "false".equals(config.path("id.token.claim").asString())
                && "false".equals(config.path("userinfo.token.claim").asString())
                && "true".equals(config.path("introspection.token.claim").asString());
    }

    private void mapScopeRoles(
            JsonNode scope,
            List<KeycloakClientRole> roles,
            String accessToken) throws IOException, InterruptedException {
        var scopeId = scope.required("id").asString();
        for (var role : roles) {
            var body = objectMapper.writeValueAsBytes(List.of(role.representation()));
            ensureSuccess(send(HttpRequest.newBuilder(scopeRoleMappingUri(
                                    scopeId, role.clientInternalId()))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofByteArray(body)), accessToken)
                    .statusCode());
        }
    }

    private void reconcileBrowserScopes(
            List<ResolvedClient> clients,
            JsonNode scope,
            String accessToken) throws IOException, InterruptedException {
        var scopeId = scope.required("id").asString();
        for (var client : clients.stream().filter(ResolvedClient::isBrowser).toList()) {
            var attached = false;
            for (var current : attachedScopes(defaultScopesUri(client), accessToken)) {
                var currentId = current.required("id").asString();
                if (scopeId.equals(currentId)) {
                    attached = true;
                } else if (KEYCLOAK_BASIC_SCOPE.equals(current.path("name").asString())) {
                    continue;
                } else {
                    delete(defaultScopeUri(client, currentId), accessToken);
                }
            }
            for (var optional : attachedScopes(optionalScopesUri(client), accessToken)) {
                delete(optionalScopeUri(
                        client, optional.required("id").asString()), accessToken);
            }
            if (!attached) {
                ensureSuccess(send(HttpRequest.newBuilder(defaultScopeUri(client, scopeId))
                                .PUT(HttpRequest.BodyPublishers.noBody()), accessToken)
                        .statusCode());
            }
        }
    }

    private List<JsonNode> attachedScopes(URI uri, String accessToken)
            throws IOException, InterruptedException {
        var response = send(HttpRequest.newBuilder(uri).GET(), accessToken);
        ensureSuccess(response.statusCode());
        return nodes(response.body());
    }

    private void delete(URI uri, String accessToken) throws IOException, InterruptedException {
        ensureSuccess(send(HttpRequest.newBuilder(uri).DELETE(), accessToken).statusCode());
    }

    private Map<String, Object> roleRepresentation(Membership membership) {
        return Map.of(
                "name", ACCESS_ROLE,
                "description", "IdentityHub private membership audience marker",
                "attributes", Map.of(
                        MANAGED_ATTRIBUTE, List.of("true"),
                        APPLICATION_ATTRIBUTE,
                        List.of(membership.applicationRef().value().toString())));
    }

    private Map<String, Object> scopeRepresentation(Membership membership, String scopeId) {
        var representation = new LinkedHashMap<String, Object>();
        if (scopeId != null) {
            representation.put("id", scopeId);
        }
        representation.put("name", scopeName(membership));
        representation.put("description", "IdentityHub private application access scope");
        representation.put("protocol", "openid-connect");
        representation.put("attributes", Map.of(
                MANAGED_ATTRIBUTE, "true",
                APPLICATION_ATTRIBUTE, membership.applicationRef().value().toString(),
                "include.in.token.scope", "false",
                "display.on.consent.screen", "false"));
        representation.put("protocolMappers", List.of(
                Map.of(
                        "name", "identityhub-audience-resolve",
                        "protocol", "openid-connect",
                        "protocolMapper", "oidc-audience-resolve-mapper",
                        "consentRequired", false,
                        "config", Map.of(
                                "access.token.claim", "true",
                                "id.token.claim", "false",
                                "introspection.token.claim", "true")),
                Map.of(
                        "name", "identityhub-public-roles",
                        "protocol", "openid-connect",
                        "protocolMapper", "oidc-hardcoded-claim-mapper",
                        "consentRequired", false,
                        "config", Map.of(
                                "claim.name", "roles",
                                "claim.value", "[]",
                                "jsonType.label", "JSON",
                                "access.token.claim", "true",
                                "id.token.claim", "false",
                                "userinfo.token.claim", "false",
                                "introspection.token.claim", "true"))));
        return representation;
    }

    private HttpResponse<String> send(HttpRequest.Builder request, String accessToken)
            throws IOException, InterruptedException {
        return httpClient.send(request.timeout(REQUEST_TIMEOUT)
                        .header("Authorization", "Bearer " + accessToken)
                        .build(), HttpResponse.BodyHandlers.ofString());
    }

    private List<JsonNode> nodes(String body) {
        return objectMapper.readValue(body, new TypeReference<List<JsonNode>>() { });
    }

    private void ensureArrayOwnership(JsonNode attributes, UUID applicationId) {
        if (!contains(attributes.path(MANAGED_ATTRIBUTE), "true")
                || !contains(attributes.path(APPLICATION_ATTRIBUTE), applicationId.toString())) {
            throw conflict();
        }
    }

    private boolean contains(JsonNode values, String expected) {
        for (var value : values) {
            if (expected.equals(value.asString())) {
                return true;
            }
        }
        return false;
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

    private MembershipProjectionException conflict() {
        return MembershipProjectionException.permanent(
                MembershipProjectionFailureCode.TOKEN_CONFIGURATION_CONFLICT, null);
    }

    private URI tokenUri() {
        return baseUri.resolve("/realms/" + encode(realm)
                + "/protocol/openid-connect/token");
    }

    private URI clientCollectionUri() {
        return baseUri.resolve("/admin/realms/" + encode(realm) + "/clients");
    }

    private URI roleCollectionUri(ResolvedClient client) {
        return URI.create(clientCollectionUri() + "/" + encode(client.internalId()) + "/roles");
    }

    private URI roleUri(ResolvedClient client) {
        return URI.create(roleCollectionUri(client) + "/" + encode(ACCESS_ROLE));
    }

    private URI clientScopeCollectionUri() {
        return baseUri.resolve("/admin/realms/" + encode(realm) + "/client-scopes");
    }

    private URI clientScopeUri(String scopeId) {
        return URI.create(clientScopeCollectionUri() + "/" + encode(scopeId));
    }

    private URI scopeRoleMappingUri(String scopeId, String clientInternalId) {
        return URI.create(clientScopeUri(scopeId) + "/scope-mappings/clients/"
                + encode(clientInternalId));
    }

    private URI defaultScopesUri(ResolvedClient client) {
        return URI.create(clientCollectionUri() + "/" + encode(client.internalId())
                + "/default-client-scopes");
    }

    private URI defaultScopeUri(ResolvedClient client, String scopeId) {
        return URI.create(defaultScopesUri(client) + "/" + encode(scopeId));
    }

    private URI optionalScopesUri(ResolvedClient client) {
        return URI.create(clientCollectionUri() + "/" + encode(client.internalId())
                + "/optional-client-scopes");
    }

    private URI optionalScopeUri(ResolvedClient client, String scopeId) {
        return URI.create(optionalScopesUri(client) + "/" + encode(scopeId));
    }

    private String keycloakClientId(ApplicationTokenClient client) {
        if (client.isApi()) {
            return client.audience();
        }
        return "ih-" + client.type().toLowerCase(java.util.Locale.ROOT) + "-" + client.id();
    }

    private String scopeName(Membership membership) {
        return "ih-access-" + membership.applicationRef().value();
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

    private record ResolvedClient(ApplicationTokenClient client, String internalId) {

        private boolean isApi() {
            return client.isApi();
        }

        private boolean isBrowser() {
            return client.isBrowser();
        }
    }

}
