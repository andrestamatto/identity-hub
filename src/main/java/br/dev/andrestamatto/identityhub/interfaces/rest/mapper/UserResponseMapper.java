package br.dev.andrestamatto.identityhub.interfaces.rest.mapper;

import br.dev.andrestamatto.identityhub.domain.entities.User;
import br.dev.andrestamatto.identityhub.domain.valueobjects.Permission;
import br.dev.andrestamatto.identityhub.domain.valueobjects.Role;
import br.dev.andrestamatto.identityhub.interfaces.rest.response.RegisteredUserResponse;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class UserResponseMapper {

    public RegisteredUserResponse registeredUserResponseFrom(User registeredUser) {
        return Optional.ofNullable(registeredUser)
                .map((validRegisteredUser) ->
                        new RegisteredUserResponse(
                                validRegisteredUser.uuid().toString(),
                                validRegisteredUser.username().value(),
                                validRegisteredUser.status().toString(),
                                userRolesSetToStringSet(validRegisteredUser.roles()),
                                userPermissionSetToStringSet(validRegisteredUser.permissions()),
                                validRegisteredUser.createdAt().toString()
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
