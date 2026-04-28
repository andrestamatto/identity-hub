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
    private final IdentityHubSecurityProperties securityProperties;

    public SecurityConfig(JwtService jwtService, IdentityHubSecurityProperties securityProperties) {
        this.jwtService = jwtService;
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
                .forEach(rule -> applyAccessRule(auth, rule));
        auth.anyRequest().authenticated();
    }

    private void applyAccessRule(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth, ConfiguredAccessRule rule) {
        switch (rule.type()) {
            case PERMIT_ALL -> auth.requestMatchers(rule.pattern()).permitAll();
            case DENY_ALL -> auth.requestMatchers(rule.pattern()).denyAll();
            case AUTHENTICATED -> auth.requestMatchers(rule.pattern()).authenticated();
            case ROLE -> auth.requestMatchers(rule.pattern()).hasRole(rule.values()[0]);
            case PERM -> auth.requestMatchers(rule.pattern()).hasAuthority("PERM_" + rule.values()[0]);
            case ANY_ROLE -> auth.requestMatchers(rule.pattern()).hasAnyRole(rule.values());
            case ANY_PERM -> auth.requestMatchers(rule.pattern()).hasAnyAuthority(
                    Arrays.stream(rule.values()).map(permission -> "PERM_" + permission).toArray(String[]::new)
            );
            case ANY_AUTHORITY -> auth.requestMatchers(rule.pattern()).hasAnyAuthority(rule.values());
            case ALL_ROLE -> auth.requestMatchers(rule.pattern()).access(hasAllAuthoritiesWithPrefix(rule.values(), "ROLE_"));
            case ALL_PERM -> auth.requestMatchers(rule.pattern()).access(hasAllAuthoritiesWithPrefix(rule.values(), "PERM_"));
            case HAS_IP -> auth.requestMatchers(rule.pattern()).access(hasAnyIp(rule.values()));
        }
    }

    private org.springframework.security.authorization.AuthorizationManager<RequestAuthorizationContext> hasAllAuthoritiesWithPrefix(
            String[] values,
            String prefix
    ) {
        Set<String> requiredAuthorities = Arrays.stream(values)
                .map(value -> prefix + value)
                .collect(Collectors.toUnmodifiableSet());

        return (authentication, context) -> {
            var currentAuthentication = authentication.get();
            if (currentAuthentication == null) {
                return new AuthorizationDecision(false);
            }

            Set<String> grantedAuthorities = currentAuthentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toUnmodifiableSet());

            return new AuthorizationDecision(grantedAuthorities.containsAll(requiredAuthorities));
        };
    }

    private org.springframework.security.authorization.AuthorizationManager<RequestAuthorizationContext> hasAnyIp(String[] allowedIps) {
        Set<String> allowedIpSet = Arrays.stream(allowedIps).collect(Collectors.toUnmodifiableSet());

        return (authentication, context) -> {
            String remoteAddress = context.getRequest().getRemoteAddr();
            return new AuthorizationDecision(allowedIpSet.contains(remoteAddress));
        };
    }
}
