package br.dev.andrestamatto.identityhub.infrastructure.security.config;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SecurityRules {

    public void applyAccessRule(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth, ConfiguredAccessRule rule) {
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

    public org.springframework.security.authorization.AuthorizationManager<RequestAuthorizationContext> hasAllAuthoritiesWithPrefix(
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

    public org.springframework.security.authorization.AuthorizationManager<RequestAuthorizationContext> hasAnyIp(String[] allowedIps) {
        Set<String> allowedIpSet = Arrays.stream(allowedIps).collect(Collectors.toUnmodifiableSet());

        return (authentication, context) -> {
            String remoteAddress = context.getRequest().getRemoteAddr();
            return new AuthorizationDecision(allowedIpSet.contains(remoteAddress));
        };
    }

}
