package br.dev.andrestamatto.identityhub.interfaces.rest.config;

import br.dev.andrestamatto.identityhub.interfaces.rest.mapper.UserResponseMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewaysConfiguration {

    @Bean
    public UserResponseMapper registerUserResponseMapper() {
        return new UserResponseMapper();
    }

}
