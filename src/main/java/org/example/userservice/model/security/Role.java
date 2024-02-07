package org.example.userservice.model.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@RequiredArgsConstructor
@Getter
public enum Role {

    USER(Set.of(Permission.READ)),
    ADMIN(Set.of(Permission.READ, Permission.CREATE, Permission.UPDATE, Permission.DELETE));

    private final Set<Permission> permissions;

    public Set<Permission> getAuthorities() {
        return getPermissions();
    }
}
