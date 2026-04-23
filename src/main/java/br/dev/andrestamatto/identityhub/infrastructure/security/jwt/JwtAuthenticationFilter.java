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
import java.util.List;
import java.util.Optional;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtIssuer jwtIssuer;

    public JwtAuthenticationFilter(JwtIssuer jwtIssuer) {
        this.jwtIssuer = jwtIssuer;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
                .filter(header -> header.startsWith(BEARER_PREFIX))
                .map(header -> header.substring(BEARER_PREFIX.length()))
                .filter(jwtIssuer::isValid)
                .ifPresent(token -> {
                    var authentication = createAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });

        filterChain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken createAuthentication(String token) {
        
        var claims = jwtIssuer.extractClaims(token);
        var userId = claims.getSubject();

        // Uso de Collection<?> para segurança de tipos antes do stream
        var rolesClaim = claims.get("roles", List.class);

        List<SimpleGrantedAuthority> authorities = (rolesClaim == null) ? List.of() :
                ((List<?>) rolesClaim).stream()
                .map(role -> new SimpleGrantedAuthority(String.valueOf(role)))
                .toList();

        // Principal: userId (UUID), Credentials: null, Authorities
        return new UsernamePasswordAuthenticationToken(userId, null, authorities);
    }
}
