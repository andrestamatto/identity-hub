package br.dev.andrestamatto.identityhub.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import java.util.UUID;

@Entity
@Table(name = "external_users")
@Getter
public class ExternalUserEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "encoded_password", nullable = false)
    private String encodedPassword;

    @Column(nullable = false)
    private String roles;

    @Column(nullable = false)
    private String permissions;

    protected ExternalUserEntity() {
    }

}
