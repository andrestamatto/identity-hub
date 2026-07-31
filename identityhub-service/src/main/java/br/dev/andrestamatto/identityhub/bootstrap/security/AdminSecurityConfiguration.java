package br.dev.andrestamatto.identityhub.bootstrap.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration(proxyBeanMethods = false)
class AdminSecurityConfiguration {

    private static final String LIVENESS_ENDPOINT = "/actuator/health/liveness";
    private static final String READINESS_ENDPOINT = "/actuator/health/readiness";
    private static final String ADMIN_RUNTIME_ENDPOINT = "/internal/admin/runtime";
    private static final String CLIENT_APPLICATION_ENDPOINTS =
            "/internal/admin/client-applications/**";
    private static final String ADMIN_ENDPOINTS = "/internal/admin/**";

    @Bean
    JwtDecoder adminJwtDecoder(AdminSecurityProperties properties) {
        var decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri().toString())
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        var issuerValidator = JwtValidators.createDefaultWithIssuer(properties.issuerUri().toString());
        var audienceValidator = new AdminAudienceValidator(properties.audience());
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
        return decoder;
    }

    @Bean
    SecurityFilterChain adminSecurityFilterChain(
            HttpSecurity http,
            JwtDecoder adminJwtDecoder,
            AdminAccessAuditFilter adminAccessAuditFilter) {
        AuthorizationManager<RequestAuthorizationContext> readAccess = AuthorizationManagers.allOf(
                AuthorityAuthorizationManager.hasAnyRole("PLATFORM_ADMIN", "PLATFORM_AUDITOR"),
                AuthorityAuthorizationManager.hasAuthority("MFA_TOTP"));
        AuthorizationManager<RequestAuthorizationContext> mutationAccess = AuthorizationManagers.allOf(
                AuthorityAuthorizationManager.hasRole("PLATFORM_ADMIN"),
                AuthorityAuthorizationManager.hasAuthority("MFA_TOTP"));

        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(LIVENESS_ENDPOINT, READINESS_ENDPOINT).permitAll()
                        .requestMatchers(HttpMethod.GET, ADMIN_RUNTIME_ENDPOINT).access(readAccess)
                        .requestMatchers(HttpMethod.GET, CLIENT_APPLICATION_ENDPOINTS).access(readAccess)
                        .requestMatchers(ADMIN_ENDPOINTS).access(mutationAccess)
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(adminJwtDecoder)
                                .jwtAuthenticationConverter(new AdminJwtAuthenticationConverter())))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(context -> context.requireExplicitSave(true))
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);

        http.addFilterAfter(adminAccessAuditFilter, AnonymousAuthenticationFilter.class);

        return http.build();
    }
}
