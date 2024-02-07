package org.example.userservice.model.security;

import org.springframework.security.core.GrantedAuthority;

public enum Permission implements GrantedAuthority {

    READ,
    CREATE,
    UPDATE,
    DELETE;

    @Override
    public String getAuthority() {
        return name();
    }
}
