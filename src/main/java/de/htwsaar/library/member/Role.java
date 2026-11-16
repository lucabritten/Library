package de.htwsaar.library.member;

import java.util.Set;

public enum Role {
    USER(Set.of("loans:write")),
    ADMIN(Set.of("loans:write", "catalog:write", "admin"));

    private final Set<String> scopes;

    Role(Set<String> scopes) {
        this.scopes = scopes;
    }

    public Set<String> scopes() {
        return scopes;
    }
}