package br.dev.andrestamatto.identityhub.integration.autoconfigure;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Auto-configuration for the safe Servlet Resource Server defaults of Integration Mode.
 */
@AutoConfiguration(before = {
    SecurityAutoConfiguration.class,
    OAuth2ResourceServerAutoConfiguration.class
})
@ConditionalOnClass({HttpSecurity.class, JwtDecoder.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "identityhub.security", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(IdentityHubSecurityProperties.class)
public class IdentityHubSecurityAutoConfiguration {

    @Bean("identityHubJwtValidationClock")
    @ConditionalOnMissingBean(name = "identityHubJwtValidationClock")
    Clock identityHubJwtValidationClock() {
        return Clock.systemUTC();
    }

    @Bean("identityHubJwtValidator")
    @ConditionalOnMissingBean(name = "identityHubJwtValidator")
    OAuth2TokenValidator<Jwt> identityHubJwtValidator(
            IdentityHubSecurityProperties properties,
            @Qualifier("identityHubJwtValidationClock") Clock clock) {
        var timestampValidator = new JwtTimestampValidator(properties.clockSkew());
        timestampValidator.setAllowEmptyExpiryClaim(false);
        timestampValidator.setClock(clock);
        return new DelegatingOAuth2TokenValidator<>(
                new JwtIssuerValidator(properties.issuerUri().toString()),
                timestampValidator,
                new IdentityHubAudienceValidator(properties.audience()),
                new IdentityHubPublicClaimsValidator(clock, properties.clockSkew()));
    }

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    JwtDecoder identityHubJwtDecoder(
            IdentityHubSecurityProperties properties,
            @Qualifier("identityHubJwtValidator") OAuth2TokenValidator<Jwt> validator) {
        var decoder = NimbusJwtDecoder.withIssuerLocation(properties.issuerUri().toString())
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(validator);
        return decoder;
    }

    @Bean("identityHubJwtAuthenticationConverter")
    @ConditionalOnMissingBean(name = "identityHubJwtAuthenticationConverter")
    Converter<Jwt, AbstractAuthenticationToken> identityHubJwtAuthenticationConverter(
            IdentityHubSecurityProperties properties) {
        return new IdentityHubJwtAuthenticationConverter(properties.authorities());
    }

    @Bean("identityHubAuthenticationEntryPoint")
    @ConditionalOnMissingBean(name = "identityHubAuthenticationEntryPoint")
    AuthenticationEntryPoint identityHubAuthenticationEntryPoint() {
        return new BearerTokenAuthenticationEntryPoint();
    }

    @Bean("identityHubAccessDeniedHandler")
    @ConditionalOnMissingBean(name = "identityHubAccessDeniedHandler")
    AccessDeniedHandler identityHubAccessDeniedHandler() {
        return new BearerTokenAccessDeniedHandler();
    }

    @Bean("identityHubSecurityFilterChain")
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    SecurityFilterChain identityHubSecurityFilterChain(
            HttpSecurity http,
            JwtDecoder jwtDecoder,
            @Qualifier("identityHubJwtAuthenticationConverter")
            Converter<Jwt, AbstractAuthenticationToken> authenticationConverter,
            @Qualifier("identityHubAuthenticationEntryPoint") AuthenticationEntryPoint authenticationEntryPoint,
            @Qualifier("identityHubAccessDeniedHandler") AccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(authenticationConverter)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(context -> context.requireExplicitSave(true))
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
