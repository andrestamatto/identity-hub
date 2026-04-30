package br.dev.andrestamatto.identityhub.infrastructure.security.config;

import br.dev.andrestamatto.identityhub.infrastructure.security.jwt.JwtAuthenticationFilter;
import br.dev.andrestamatto.identityhub.infrastructure.security.jwt.JwtProperties;
import br.dev.andrestamatto.identityhub.infrastructure.security.jwt.JwtService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, IdentityHubSecurityProperties.class})
public class SecurityConfig {

    private final JwtService jwtService;
    private final SecurityRules securityRules;
    private final IdentityHubSecurityProperties securityProperties;

    public SecurityConfig(JwtService jwtService, SecurityRules securityRules, IdentityHubSecurityProperties securityProperties) {
        this.jwtService = jwtService;
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
                    new JwtAuthenticationFilter(jwtService),
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
