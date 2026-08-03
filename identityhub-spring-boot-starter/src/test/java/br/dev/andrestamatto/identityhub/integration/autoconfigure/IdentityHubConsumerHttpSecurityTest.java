package br.dev.andrestamatto.identityhub.integration.autoconfigure;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(
        classes = IdentityHubConsumerHttpSecurityTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class IdentityHubConsumerHttpSecurityTest {

    private static final TestJwtIssuer JWT_ISSUER = startJwtIssuer();

    @Autowired
    private MockMvc mvc;

    @AfterAll
    static void closeIssuer() {
        JWT_ISSUER.close();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("identityhub.security.issuer-uri", JWT_ISSUER::issuer);
        registry.add("identityhub.security.audience", () -> "catalog-api");
        registry.add("identityhub.security.allow-http-for-loopback", () -> true);
    }

    @Test
    void deniesAValidTokenWithoutTheScopeRequiredByTheConsumer() throws Exception {
        var token = token("catalog:read", List.of());

        mvc.perform(get("/scope-protected").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void mapsOnlyPublicRolesAndScopesForTheConsumerChain() throws Exception {
        var privateRoleToken = token(
                "catalog:read",
                List.of(),
                Map.of("realm_access", Map.of("roles", List.of("catalog-admin"))));
        var publicRoleToken = token("catalog:read", List.of("catalog-admin"));
        var scopeToken = token("catalog:write", List.of());

        mvc.perform(get("/role-protected").header(HttpHeaders.AUTHORIZATION, "Bearer " + privateRoleToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/role-protected").header(HttpHeaders.AUTHORIZATION, "Bearer " + publicRoleToken))
                .andExpect(status().isOk())
                .andExpect(content().string("role-protected"));
        mvc.perform(get("/scope-protected").header(HttpHeaders.AUTHORIZATION, "Bearer " + scopeToken))
                .andExpect(status().isOk())
                .andExpect(content().string("scope-protected"));
    }

    private String token(String scope, List<String> roles) throws Exception {
        return token(scope, roles, Map.of());
    }

    private String token(String scope, List<String> roles, Map<String, Object> additionalClaims) throws Exception {
        var now = Instant.now();
        return JWT_ISSUER.issueAccessToken(
                JWT_ISSUER.issuer(),
                List.of("catalog-api"),
                now,
                now.plusSeconds(300),
                scope,
                roles,
                additionalClaims);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({ConsumerSecurityConfiguration.class, ProtectedEndpoints.class})
    static class TestApplication {
    }

    @Configuration(proxyBeanMethods = false)
    static class ConsumerSecurityConfiguration {

        @Bean
        SecurityFilterChain consumerSecurityFilterChain(
                HttpSecurity http,
                JwtDecoder jwtDecoder,
                @Qualifier("identityHubJwtAuthenticationConverter")
                Converter<Jwt, AbstractAuthenticationToken> authenticationConverter) throws Exception {
            http
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers("/scope-protected").hasAuthority("SCOPE_catalog:write")
                            .requestMatchers("/role-protected").hasAuthority("ROLE_catalog-admin")
                            .anyRequest().authenticated())
                    .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                            .decoder(jwtDecoder)
                            .jwtAuthenticationConverter(authenticationConverter)))
                    .sessionManagement(session -> session
                            .sessionCreationPolicy(SessionCreationPolicy.STATELESS));
            return http.build();
        }
    }

    @RestController
    static class ProtectedEndpoints {

        @GetMapping("/scope-protected")
        String scopeProtected() {
            return "scope-protected";
        }

        @GetMapping("/role-protected")
        String roleProtected() {
            return "role-protected";
        }
    }

    private static TestJwtIssuer startJwtIssuer() {
        try {
            return TestJwtIssuer.start();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not start the local JWT issuer", exception);
        }
    }
}
