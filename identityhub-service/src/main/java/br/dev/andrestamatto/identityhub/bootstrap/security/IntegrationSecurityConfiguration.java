package br.dev.andrestamatto.identityhub.bootstrap.security;

import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
class IntegrationSecurityConfiguration {

    private static final String INTEGRATION_ENDPOINTS = "/integration/**";
    private static final String ONBOARDING_SESSIONS =
            "/integration/v1/onboarding-sessions";

    @Bean
    @Order(1)
    SecurityFilterChain integrationSecurityFilterChain(
            HttpSecurity http,
            AdminSecurityProperties identityProvider,
            IntegrationSecurityProperties integration) {
        var decoder = NimbusJwtDecoder.withJwkSetUri(identityProvider.jwkSetUri().toString())
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        var issuerValidator = JwtValidators.createDefaultWithIssuer(
                identityProvider.issuerUri().toString());
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                issuerValidator,
                new IntegrationAudienceValidator(integration.audience())));

        http
                .securityMatcher(INTEGRATION_ENDPOINTS)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, ONBOARDING_SESSIONS)
                        .hasAuthority("SCOPE_onboarding:write")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .decoder(decoder)
                        .jwtAuthenticationConverter(
                                new IntegrationJwtAuthenticationConverter())))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(context -> context.requireExplicitSave(true))
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
