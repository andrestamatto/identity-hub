package br.dev.andrestamatto.identityhub.interfaces.rest.mapper;

import br.dev.andrestamatto.identityhub.domain.entities.User;
import br.dev.andrestamatto.identityhub.domain.valueobjects.Permission;
import br.dev.andrestamatto.identityhub.domain.valueobjects.Role;
import br.dev.andrestamatto.identityhub.domain.valueobjects.VerificationToken;
import br.dev.andrestamatto.identityhub.interfaces.rest.response.UserResponse;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class UserResponseMapper {

    public UserResponse from(User user) {
        return Optional.ofNullable(user)
                .map((validRegisteredUser) ->
                        new UserResponse(
                                String.valueOf(validRegisteredUser.uuid()),
                                validRegisteredUser.username().value(),
                                String.valueOf(validRegisteredUser.status()),
                                userRolesSetToStringSet(validRegisteredUser.roles()),
                                userPermissionSetToStringSet(validRegisteredUser.permissions()),
                                String.valueOf(validRegisteredUser.createdAt()),
                                Optional.ofNullable(validRegisteredUser.verificationToken()).
                                                map(VerificationToken::method)
                                                .map(String::valueOf)
                                                .orElse(null),
                                Optional.ofNullable(validRegisteredUser.verificationToken()).
                                                map(VerificationToken::expiresAt)
                                                .map(String::valueOf)
                                                .orElse(null)
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
