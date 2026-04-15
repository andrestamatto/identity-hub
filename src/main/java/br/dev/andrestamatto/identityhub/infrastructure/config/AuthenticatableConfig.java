package br.dev.andrestamatto.identityhub.infrastructure.config;

import br.dev.andrestamatto.identityhub.application.usecase.Authenticatable;
import br.dev.andrestamatto.identityhub.application.usecase.Login;
import br.dev.andrestamatto.identityhub.domain.service.AuthProvider;
import br.dev.andrestamatto.identityhub.domain.service.LoginProvider;
import br.dev.andrestamatto.identityhub.infrastructure.security.JwtIssuer;
import br.dev.andrestamatto.identityhub.infrastructure.security.TokenIssuer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthenticatableConfig {

    @Bean
    public AuthProvider loginProvider(){
        return new LoginProvider();
    }

    @Bean
    public TokenIssuer jwtIssuer() {
        return new JwtIssuer();
    }

    @Bean
    public Authenticatable login(AuthProvider authProvider, TokenIssuer jwtIssuer) {
        return new Login(authProvider, jwtIssuer);
    }

}
