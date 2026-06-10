package br.dev.andrestamatto.identityhub.infrastructure.repository.mapper;

import br.dev.andrestamatto.identityhub.domain.entities.User;
import br.dev.andrestamatto.identityhub.domain.valueobjects.EncodedPassword;
import br.dev.andrestamatto.identityhub.domain.valueobjects.NotificationMethod;
import br.dev.andrestamatto.identityhub.domain.valueobjects.VerificationToken;
import br.dev.andrestamatto.identityhub.domain.valueobjects.Username;
import br.dev.andrestamatto.identityhub.domain.valueobjects.UsernameType;
import br.dev.andrestamatto.identityhub.infrastructure.repository.entity.UserJpaEntity;

import java.util.Optional;

public final class UserEntityMapper {

    public static UserJpaEntity jpaEntityFrom(User user) {
        return Optional.ofNullable(user)
                .map((validUser) -> {
                    return UserJpaEntity.of(
                        validUser.uuid(),
                        validUser.username().value(),
                        validUser.username().usernameType().name(),
                        validUser.encodedPassword().value(),
                        validUser.status(),
                        validUser.createdAt(),
                        validUser.updatedAt(),
                        verificationTokenCodeFrom(validUser),
                        verificationTokenMethodFrom(validUser),
                        verificationTokenExpiresAtFrom(validUser)
                    );
                })
                .orElseThrow();
    }

    public static User toDomain(UserJpaEntity jpaUserEntity) {
        return Optional.ofNullable(jpaUserEntity)
                .map( (validJpaUserEntity) -> {
                    return User.fromPersistence(
                            validJpaUserEntity.getId(),
                            Username.create(
                                    validJpaUserEntity.getUsername(),
                                    UsernameType.valueOf(validJpaUserEntity.getUsernameType())
                            ),
                            EncodedPassword.create(validJpaUserEntity.getEncodedPassword()),
                            validJpaUserEntity.getStatus(),
                            validJpaUserEntity.getCreatedAt(),
                            validJpaUserEntity.getUpdatedAt(),
                            verificationTokenFrom(validJpaUserEntity)
                    );
                })
                .orElseThrow();
    }

    private static String verificationTokenCodeFrom(User user) {
        return Optional.ofNullable(user.verificationToken())
                .map(VerificationToken::code)
                .orElse(null);
    }

    private static NotificationMethod verificationTokenMethodFrom(User user) {
        return Optional.ofNullable(user.verificationToken())
                .map(VerificationToken::method)
                .orElse(null);
    }

    private static java.time.Instant verificationTokenExpiresAtFrom(User user) {
        return Optional.ofNullable(user.verificationToken())
                .map(VerificationToken::expiresAt)
                .orElse(null);
    }

    private static VerificationToken verificationTokenFrom(UserJpaEntity userJpaEntity) {
        if (userJpaEntity.getVerificationTokenCode() == null) {
            return null;
        }

        return new VerificationToken(
                userJpaEntity.getVerificationTokenCode(),
                userJpaEntity.getVerificationTokenMethod(),
                userJpaEntity.getVerificationTokenExpiresAt()
        );
    }

}
