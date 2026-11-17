package de.htwsaar.library;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class TestTokens {
    private TestTokens() { }

    public static RequestPostProcessor asMember(long id) {
        return jwt().jwt(j -> j.subject(String.valueOf(id)))
                .authorities(new SimpleGrantedAuthority("SCOPE_loans:write"));
    }

    public static RequestPostProcessor asAdmin() {
        return jwt().jwt(j -> j.subject("1")).authorities(
                new SimpleGrantedAuthority("SCOPE_catalog:write"),
                new SimpleGrantedAuthority("SCOPE_admin"),
                new SimpleGrantedAuthority("SCOPE_loans:write"));
    }
}