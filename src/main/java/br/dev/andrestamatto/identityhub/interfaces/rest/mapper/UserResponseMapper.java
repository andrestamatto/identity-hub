package br.dev.andrestamatto.identityhub.interfaces.rest.mapper;

import br.dev.andrestamatto.identityhub.domain.entities.User;
import br.dev.andrestamatto.identityhub.domain.valueobjects.Permission;
import br.dev.andrestamatto.identityhub.domain.valueobjects.Role;
import br.dev.andrestamatto.identityhub.domain.valueobjects.VerificationToken;
import br.dev.andrestamatto.identityhub.interfaces.rest.response.RegisteredUserResponse;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class UserResponseMapper {

    public RegisteredUserResponse registeredUserResponseFrom(User registeredUser) {
        return Optional.ofNullable(registeredUser)
                .map((validRegisteredUser) ->
                        new RegisteredUserResponse(
                                String.valueOf(validRegisteredUser.uuid()),
                                String.valueOf(validRegisteredUser.username()),
                                String.valueOf(validRegisteredUser.status()),
                                userRolesSetToStringSet(validRegisteredUser.roles()),
                                userPermissionSetToStringSet(validRegisteredUser.permissions()),
                                String.valueOf(validRegisteredUser.createdAt()),
                                String.valueOf(
                                        Optional.ofNullable(validRegisteredUser.verificationToken()).
                                                map(VerificationToken::method)
                                                .orElse(null)
                                ),
                                String.valueOf(
                                        Optional.ofNullable(validRegisteredUser.verificationToken()).
                                                map(VerificationToken::expiresAt)
                                                .orElse(null)
                                )
                    )
                ).orElseThrow();
    }

    private Set<String> userRolesSetToStringSet(Set<Role> userRoles) {
        return Optional.ofNullable(userRoles).map(
                validUserRoles -> {
                    return validUserRoles.stream().map(Role::roleName).collect(Collectors.toSet());
                }
        ).orElse(Set.of());
    }

    private Set<String> userPermissionSetToStringSet(Set<Permission> userPermissions) {
        return Optional.ofNullable(userPermissions).map(
                validUserPermissions -> {
                    return validUserPermissions.stream().map(Permission::slug).collect(Collectors.toSet());
                })
                .orElse(Set.of());
    }

}
