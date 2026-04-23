package br.dev.andrestamatto.identityhub.infrastructure.security.config;

import br.dev.andrestamatto.identityhub.infrastructure.security.TokenIssuer;
import br.dev.andrestamatto.identityhub.infrastructure.security.jwt.JwtAuthenticationFilter;
import br.dev.andrestamatto.identityhub.infrastructure.security.jwt.JwtIssuer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final TokenIssuer jwtIssuer;

    public SecurityConfig(TokenIssuer jwtIssuer) {
        this.jwtIssuer = jwtIssuer;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        final String AUTH_REQUEST_MATCHERS = "/auth/**";


        http
                .csrf(CsrfConfigurer::disable)
                .formLogin(FormLoginConfigurer<HttpSecurity>::disable)
                .httpBasic(HttpBasicConfigurer<HttpSecurity>::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(AUTH_REQUEST_MATCHERS).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                    new JwtAuthenticationFilter((JwtIssuer) jwtIssuer),
                    UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}

