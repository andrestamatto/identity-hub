package br.dev.andrestamatto.identityhub.infrastructure.repository.mapper;

import br.dev.andrestamatto.identityhub.domain.valueobjects.UserStatus;
import br.dev.andrestamatto.identityhub.support.UserTestData;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UserEntityMapperTest {

    @Test
    public void shouldPreserveVerificationTokenWhenMappingUserToJpaEntityAndBackToDomain() {
        var clock = Clock.fixed(Instant.parse("2099-05-28T10:00:00Z"), ZoneOffset.UTC);
        var verificationToken = UserTestData.createDefaultValidVerificationToken(
                UserTestData.validVerificationCode,
                clock
        );
        var pendingUser = UserTestData.createUser(
                UserTestData.registeredWithVerificationToken(verificationToken),
                UserStatus.PENDING_VERIFICATION
        );

        var jpaEntity = UserEntityMapper.jpaEntityFrom(pendingUser);
        var mappedUser = UserEntityMapper.toDomain(jpaEntity);

        assertNotNull(mappedUser.verificationToken());
        assertEquals(verificationToken.code(), mappedUser.verificationToken().code());
        assertEquals(verificationToken.method(), mappedUser.verificationToken().method());
        assertEquals(verificationToken.expiresAt(), mappedUser.verificationToken().expiresAt());
    }
}
