package br.dev.andrestamatto.identityhub.support;

import br.dev.andrestamatto.identityhub.domain.entities.User;
import br.dev.andrestamatto.identityhub.domain.valueobjects.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class UserTestData {

    public static final String validUsernameString = "user1@identityhub.com";
    public static final String validRawPasswordString = "Password@123";
    public static final String validEncodedPasswordString = "$2a$12$R9h/cIPz0gi.UR3XvMhoHeM3N2fU3s.8k3cT6K7qgI8c/bK16A9i6";
    public static final ObjectMapper userMapper = new ObjectMapper();

    private static User resultUser;

    private UserTestData() {}

    public static User registered() {
        return new User(
                UUID.randomUUID(),
                null,
                validEmailUsername(),
                new EncodedPassword(validEncodedPasswordString),
                null,
                customerRoleAsSet(),
                customerPermissionsAsSet(),
                null,  // 0 failedLoginAttempts
                0,
                null,
                null,
                Instant.now(),
                null,
                null,
                null
        );
    }

    public static User createUser(User withUser, UserStatus withUserStatus) {
        return User.create(withUser, withUserStatus);
    }

    public static String registeredAsJsonString() {
        try {
            return userMapper.writeValueAsString(registered());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Username validEmailUsername(){
        return Username.create(validUsernameString);
    }

    public static Set<Role> customerRoleAsSet() {
        return Set.of(
            new Role(
                UUID.randomUUID(),
       "CUSTOMER",
   "Default user of the system.",
                Instant.now()
            )
        );
    }

    public static Set<Permission> customerPermissionsAsSet() {
        return Set.of(
                new Permission(
                        UUID.randomUUID(),
                        "Read User",
                        "user:read",
                        "Permission to read users.",
                        Instant.now()
                ),
                new Permission(
                        UUID.randomUUID(),
                        "Write Self",
                        "self:write",
                        "Permission to save or update itself.",
                        Instant.now()
                )
        );
    }
}
