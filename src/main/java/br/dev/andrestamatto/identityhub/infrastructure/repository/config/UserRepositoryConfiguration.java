package br.dev.andrestamatto.identityhub.infrastructure.repository.config;

import br.dev.andrestamatto.identityhub.application.ports.output.UserRepository;
import br.dev.andrestamatto.identityhub.domain.entities.User;
import br.dev.andrestamatto.identityhub.domain.valueobjects.Username;
import br.dev.andrestamatto.identityhub.infrastructure.repository.JpaUserRepositoryAdapter;
import br.dev.andrestamatto.identityhub.infrastructure.repository.ports.SpringDataUserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class UserRepositoryConfiguration {


    @Bean
    @ConditionalOnProperty(prefix = "identity-hub.repository", name = "type", havingValue = "jpa")
    @ConditionalOnClass(JpaRepository.class)
    UserRepository jpaUserRepository(SpringDataUserRepository repository) {
        return new JpaUserRepositoryAdapter(repository);
    }

    @Bean
    @ConditionalOnMissingBean(UserRepository.class)
    public UserRepository inMemoryUserRepository() {
        return new UserRepository() {
            private final Map<String, User> users = new ConcurrentHashMap<>();
            @Override
            public boolean existsBy(Username username) {
                return users.containsKey(username.value());
            }

            @Override
            public User save(User user) {
                users.put(user.username().value(), user);
                return user;
            }

            @Override
            public User findByUsername(Username username) {
                return users.get(username.value());
            }
        };
    }
}
