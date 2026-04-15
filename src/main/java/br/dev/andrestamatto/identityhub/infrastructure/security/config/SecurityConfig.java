package br.dev.andrestamatto.identityhub.infrastructure.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(CsrfConfigurer::disable) // simplifica para API
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll() // libera login
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}

