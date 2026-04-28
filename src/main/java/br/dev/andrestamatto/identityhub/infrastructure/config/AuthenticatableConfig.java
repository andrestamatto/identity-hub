package br.dev.andrestamatto.identityhub.infrastructure.config;

import br.dev.andrestamatto.identityhub.application.ports.LoadExternalIdentity;
import br.dev.andrestamatto.identityhub.application.usecase.Authenticatable;
import br.dev.andrestamatto.identityhub.application.usecase.Login;
import br.dev.andrestamatto.identityhub.domain.service.AuthProvider;
import br.dev.andrestamatto.identityhub.domain.service.LoginProvider;
import br.dev.andrestamatto.identityhub.domain.service.PasswordEncoder;
import br.dev.andrestamatto.identityhub.infrastructure.security.TokenService;
import br.dev.andrestamatto.identityhub.infrastructure.security.jwt.JwtService;
import br.dev.andrestamatto.identityhub.infrastructure.security.password.BCryptPasswordEncoderAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthenticatableConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoderAdapter();
    }

    @Bean
    public AuthProvider loginProvider(PasswordEncoder passwordEncoder, LoadExternalIdentity loadExternalIdentity) {
        return new LoginProvider(passwordEncoder, loadExternalIdentity);
    }

    @Bean
    public TokenService tokenService(JwtService jwtService) {
        return jwtService;
    }

    @Bean
    public Authenticatable login(AuthProvider authProvider, TokenService tokenService) {
        return new Login(authProvider, tokenService);
    }
}
