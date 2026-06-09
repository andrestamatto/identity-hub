package br.dev.andrestamatto.identityhub.infrastructure.repository.entity;

import br.dev.andrestamatto.identityhub.domain.valueobjects.UserStatus;
import br.dev.andrestamatto.identityhub.domain.valueobjects.NotificationMethod;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "identityhub_users")
public class UserJpaEntity {
    @Id
    UUID id;

    @Column(nullable = false, unique = true)
    String username;

    @Column(nullable = false)
    String usernameType;

    @Column(nullable = false)
    String encodedPassword;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    UserStatus status;

    @Column(nullable = false)
    Instant createdAt;

    @Column
    Instant updatedAt;

    @Column
    String verificationTokenCode;

    @Column
    @Enumerated(EnumType.STRING)
    NotificationMethod verificationTokenMethod;

    @Column
    Instant verificationTokenExpiresAt;

    protected UserJpaEntity() {
        // JPA only
    }

    private UserJpaEntity(
            UUID id,
            String username,
            String usernameType,
            String encodedPassword,
            UserStatus status,
            Instant createdAt,
            Instant updatedAt,
            String verificationTokenCode,
            NotificationMethod verificationTokenMethod,
            Instant verificationTokenExpiresAt
    ) {
        this.id = id;
        this.username = username;
        this.usernameType = usernameType;
        this.encodedPassword = encodedPassword;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.verificationTokenCode = verificationTokenCode;
        this.verificationTokenMethod = verificationTokenMethod;
        this.verificationTokenExpiresAt = verificationTokenExpiresAt;
    }

    public static UserJpaEntity of(
            UUID id,
            String username,
            String usernameType,
            String encodedPassword,
            UserStatus status,
            Instant createdAt,
            Instant updatedAt,
            String verificationTokenCode,
            NotificationMethod verificationTokenMethod,
            Instant verificationTokenExpiresAt
    ) {
        return new UserJpaEntity(
                id,
                username,
                usernameType,
                encodedPassword,
                status,
                createdAt,
                updatedAt,
                verificationTokenCode,
                verificationTokenMethod,
                verificationTokenExpiresAt
        );
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getUsernameType() { return usernameType; }
    public String getEncodedPassword() { return encodedPassword; }
    public UserStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getVerificationTokenCode() { return verificationTokenCode; }
    public NotificationMethod getVerificationTokenMethod() { return verificationTokenMethod; }
    public Instant getVerificationTokenExpiresAt() { return verificationTokenExpiresAt; }

    // mudança controlada
    public void markUpdated(Instant now) {
        this.updatedAt = now;
    }
}
