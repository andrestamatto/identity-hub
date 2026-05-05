package br.dev.andrestamatto.identityhub.infrastructure.security.config;

import br.dev.andrestamatto.identityhub.application.ports.TokenServicePort;
import br.dev.andrestamatto.identityhub.infrastructure.security.jwt.JwtAuthenticationFilter;
import br.dev.andrestamatto.identityhub.infrastructure.security.jwt.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;
import java.util.Optional;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, IdentityHubSecurityProperties.class})
public class SecurityConfig {

    private final TokenServicePort tokenServicePort;
    private final SecurityRules securityRules;
    private final IdentityHubSecurityProperties securityProperties;

    public SecurityConfig(TokenServicePort tokenServicePort, SecurityRules securityRules, IdentityHubSecurityProperties securityProperties) {
        this.tokenServicePort = tokenServicePort;
        this.securityRules = securityRules;
        this.securityProperties = securityProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(CsrfConfigurer::disable)
                .formLogin(FormLoginConfigurer<HttpSecurity>::disable)
                .httpBasic(HttpBasicConfigurer<HttpSecurity>::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(this::configureAuthorizationRules)
                .addFilterBefore(
                    new JwtAuthenticationFilter(tokenServicePort),
                    UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    private void configureAuthorizationRules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        Optional.ofNullable(securityProperties.rules()).orElse(List.of()).stream()
                .map(ConfiguredAccessRule::from)
                .flatMap(Optional::stream)
                .forEach(rule -> securityRules.applyAccessRule(auth, rule));
        auth.anyRequest().authenticated();
    }

}
