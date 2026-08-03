package br.dev.andrestamatto.identityhub.bootstrap.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
class IntegrationSecurityConfiguration {

    static final String INTEGRATION_AUDIENCE = "identityhub-integration-api";
    private static final String MEMBERSHIP_ENDPOINT = "/api/v1/memberships";
    private static final String MEMBERSHIP_OPERATION_ENDPOINT =
            "/api/v1/membership-operations/*";

    @Bean
    @Order(1)
    SecurityFilterChain integrationSecurityFilterChain(
            HttpSecurity http,
            AdminSecurityProperties properties) {
        http
                .securityMatcher("/api/v1/**")
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, MEMBERSHIP_ENDPOINT)
                        .hasAuthority("SCOPE_membership:write")
                        .requestMatchers(HttpMethod.GET, MEMBERSHIP_OPERATION_ENDPOINT)
                        .hasAuthority("SCOPE_membership:write")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(
                        integrationJwtDecoder(properties))))
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

    JwtDecoder integrationJwtDecoder(AdminSecurityProperties properties) {
        var decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri().toString())
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        var issuer = JwtValidators.createDefaultWithIssuer(properties.issuerUri().toString());
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                issuer,
                new IntegrationAudienceValidator()));
        return decoder;
    }
}
