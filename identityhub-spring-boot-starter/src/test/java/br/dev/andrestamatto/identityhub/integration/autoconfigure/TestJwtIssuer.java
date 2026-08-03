package br.dev.andrestamatto.identityhub.integration.autoconfigure;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

final class TestJwtIssuer implements AutoCloseable {

    private static final String KEY_ID = "identityhub-test-key";

    private final HttpServer server;
    private final RSAKey signingKey;
    private final String issuer;

    private TestJwtIssuer(HttpServer server, RSAKey signingKey, int jwksResponseStatus) {
        this.server = server;
        this.signingKey = signingKey;
        issuer = "http://127.0.0.1:" + server.getAddress().getPort();
        server.createContext("/.well-known/openid-configuration", exchange -> respond(
                exchange,
                "{\"issuer\":\"" + issuer + "\",\"jwks_uri\":\"" + issuer + "/jwks\"}"));
        server.createContext("/jwks", exchange -> {
            if (jwksResponseStatus == 200) {
                respond(exchange, new JWKSet(signingKey.toPublicJWK()).toString());
                return;
            }
            exchange.sendResponseHeaders(jwksResponseStatus, -1);
            exchange.close();
        });
        server.start();
    }

    static TestJwtIssuer start() throws Exception {
        return startWithJwksResponse(200);
    }

    static TestJwtIssuer startWithUnavailableJwks() throws Exception {
        return startWithJwksResponse(503);
    }

    private static TestJwtIssuer startWithJwksResponse(int jwksResponseStatus) throws Exception {
        var server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        var signingKey = new RSAKeyGenerator(2048).keyID(KEY_ID).generate();
        return new TestJwtIssuer(server, signingKey, jwksResponseStatus);
    }

    String issuer() {
        return issuer;
    }

    String issueAccessToken(
            String tokenIssuer,
            List<String> audience,
            Instant issuedAt,
            Instant expiresAt,
            String scope,
            List<String> roles) throws Exception {
        return issueAccessToken(tokenIssuer, audience, issuedAt, expiresAt, scope, roles, Map.of());
    }

    String issueAccessToken(
            String tokenIssuer,
            List<String> audience,
            Instant issuedAt,
            Instant expiresAt,
            String scope,
            List<String> roles,
            Map<String, Object> additionalClaims) throws Exception {
        var claims = new JWTClaimsSet.Builder()
                .issuer(tokenIssuer)
                .subject("account-123")
                .audience(audience)
                .issueTime(java.util.Date.from(issuedAt))
                .expirationTime(java.util.Date.from(expiresAt))
                .jwtID("token-123")
                .claim("scope", scope)
                .claim("roles", roles);
        additionalClaims.forEach(claims::claim);
        var token = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(),
                claims.build());
        token.sign(new RSASSASigner(signingKey));
        return token.serialize();
    }

    String issueAccessTokenWithUnknownKey(
            List<String> audience,
            Instant issuedAt,
            Instant expiresAt) throws Exception {
        var unknownKey = new RSAKeyGenerator(2048).keyID("unknown-key").generate();
        var claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("account-123")
                .audience(audience)
                .issueTime(java.util.Date.from(issuedAt))
                .expirationTime(java.util.Date.from(expiresAt))
                .jwtID("token-123")
                .claim("scope", "catalog:read")
                .claim("roles", List.of())
                .build();
        var token = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(unknownKey.getKeyID()).build(),
                claims);
        token.sign(new RSASSASigner(unknownKey));
        return token.serialize();
    }

    String issueHs256Token(List<String> audience, Instant issuedAt, Instant expiresAt) throws Exception {
        var claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("account-123")
                .audience(audience)
                .issueTime(java.util.Date.from(issuedAt))
                .expirationTime(java.util.Date.from(expiresAt))
                .jwtID("token-123")
                .claim("scope", "catalog:read")
                .claim("roles", List.of())
                .build();
        var token = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        var secret = new byte[32];
        java.util.Arrays.fill(secret, (byte) 7);
        token.sign(new MACSigner(secret));
        return token.serialize();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String body) throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var response = exchange.getResponseBody()) {
            response.write(bytes);
        }
    }
}
