package br.dev.andrestamatto.identityhub.infrastructure.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String PERMISSION_PREFIX = "PERM_";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
                .filter(header -> header.startsWith(BEARER_PREFIX))
                .map(header -> header.substring(BEARER_PREFIX.length()))
                .filter(jwtService::isValid)
                .ifPresent(token -> {
                    var authentication = retrieveAuthenticationFromToken(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });

        filterChain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken retrieveAuthenticationFromToken(String token) {
        var claims = jwtService.extractClaims(token);
        var userId = claims.getSubject();

        var rolesClaim = claims.get("roles", List.class);
        var permissionsClaim = claims.get("permissions", List.class);

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.addAll(convertToAuthorities(rolesClaim, this::toRoleAuthority));
        authorities.addAll(convertToAuthorities(permissionsClaim, this::toPermissionAuthority));

        return new UsernamePasswordAuthenticationToken(userId, null, authorities);
    }

    private List<SimpleGrantedAuthority> convertToAuthorities(Object claim, Function<String, String> authorityMapper) {
        return Optional.ofNullable(claim)
                .filter(c -> c instanceof List)
                .map(c -> (List<?>) c)
                .orElse(Collections.emptyList())
                .stream()
                .map(item -> String.valueOf(item).trim())
                .filter(value -> !value.isBlank())
                .map(authorityMapper)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    private String toRoleAuthority(String role) {
        return normalizePrefixedAuthority(role, ROLE_PREFIX);
    }

    private String toPermissionAuthority(String permission) {
        return normalizePrefixedAuthority(permission, PERMISSION_PREFIX);
    }

    private String normalizePrefixedAuthority(String value, String prefix) {
        return value.startsWith(prefix) ? value : prefix + value;
    }
}
