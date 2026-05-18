package br.dev.andrestamatto.identityhub.domain.entities;

import br.dev.andrestamatto.identityhub.domain.valueobjects.*;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record User(
       UUID uuid,
       String name,
       Username username,
       EncodedPassword encodedPassword,
       UserStatus status,
       Set<Role> roles,
       Set<Permission> permissions,
       List<LoginAttempt> failedLoginAttempts,
       Integer failedLoginCount,
       Instant lastFailedLoginAt,
       Instant lockedUntil,
       Instant createdAt,
       Instant passwordChangedAt,
       Instant updatedAt
) {

    public User {
        if (username == null || username.value().isBlank()) {throw new IllegalArgumentException("User's username cannot be null or blank");}
        if (encodedPassword == null || encodedPassword.value().isBlank()) {throw new IllegalArgumentException("User's encoded password cannot be null or blank");}
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID uuid;
        private String name;
        private Username username;
        private EncodedPassword encodedPassword;
        private UserStatus status;
        private Set<Role> roles;
        private Set<Permission> permissions;
        private List<LoginAttempt> failedLoginAttempts;
        private Integer failedLoginCount;
        private Instant lastFailedLoginAt;
        private Instant lockedUntil;
        private Instant createdAt;
        private Instant passwordChangedAt;
        private Instant updatedAt;

        private Builder() {
        }

        public Builder uuid(UUID uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder username(Username username) {
            this.username = username;
            return this;
        }

        public Builder password(EncodedPassword encodedPassword) {
            this.encodedPassword = encodedPassword;
            return this;
        }

        public Builder status(UserStatus status) {
            this.status = status;
            return this;
        }

        public Builder roles(Set<Role> roles) {
            this.roles = roles;
            return this;
        }

        public Builder permissions(Set<Permission> permissions) {
            this.permissions = permissions;
            return this;
        }

        public Builder failedLoginAttempts(List<LoginAttempt> failedLoginAttempts) {
            this.failedLoginAttempts = failedLoginAttempts;
            return this;
        }

        public Builder failedLoginCount(Integer failedLoginCount) {
            this.failedLoginCount = failedLoginCount;
            return this;
        }

        public Builder lastFailedLoginAt(Instant lastFailedLoginAt) {
            this.lastFailedLoginAt = lastFailedLoginAt;
            return this;
        }

        public Builder lockedUntil(Instant lockedUntil) {
            this.lockedUntil = lockedUntil;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder passwordChangedAt(Instant passwordChangedAt) {
            this.passwordChangedAt = passwordChangedAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public User build() {
            return new User(
                    uuid,
                    name,
                    username,
                    encodedPassword,
                    status,
                    roles,
                    permissions,
                    failedLoginAttempts,
                    failedLoginCount,
                    lastFailedLoginAt,
                    lockedUntil,
                    createdAt,
                    passwordChangedAt,
                    updatedAt
            );
        }
    }
}
