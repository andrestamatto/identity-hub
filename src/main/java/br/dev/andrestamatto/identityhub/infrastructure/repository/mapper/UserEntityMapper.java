package br.dev.andrestamatto.identityhub.infrastructure.repository.mapper;

import br.dev.andrestamatto.identityhub.domain.entities.User;
import br.dev.andrestamatto.identityhub.domain.valueobjects.*;
import br.dev.andrestamatto.identityhub.infrastructure.repository.entity.UserEntity;

import java.util.Optional;

public final class UserEntityMapper {

    public static UserEntity jpaEntityFrom(User user) {
        return Optional.ofNullable(user)
                .map((validUser) -> {
                    return UserEntity.of(
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

    public static User toDomain(UserEntity jpaUserEntity) {
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

    private static VerificationToken verificationTokenFrom(UserEntity userEntity) {
        if (userEntity.getVerificationTokenCode() == null) {
            return null;
        }

        return new VerificationToken(
                userEntity.getVerificationTokenCode(),
                userEntity.getVerificationTokenMethod(),
                userEntity.getVerificationTokenExpiresAt()
        );
    }

}
