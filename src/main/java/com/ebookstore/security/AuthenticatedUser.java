package com.ebookstore.security;

import com.ebookstore.common.domain.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Custom {@link UserDetails} placed in the {@link org.springframework.security.core.context.SecurityContextHolder}
 * after successful JWT authentication.
 *
 * <p>Controllers retrieve the authenticated user's database id via:
 * <pre>
 *   ((AuthenticatedUser) authentication.getPrincipal()).getId()
 * </pre>
 *
 * <p>The password hash is never exposed through {@code toString()},
 * serialization, logs, or API responses.
 */
public class AuthenticatedUser implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final UserRole role;

    public AuthenticatedUser(Long id, String email, String password, UserRole role) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public UserRole getRole() {
        return role;
    }

    /** Returns the email address — used as the JWT {@code sub} claim. */
    @Override
    public String getUsername() {
        return email;
    }

    /** Returns the BCrypt password hash. Never log this value. */
    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }

    /** Password hash deliberately excluded. */
    @Override
    public String toString() {
        return "AuthenticatedUser{id=" + id + ", email='" + email + "', role=" + role + "}";
    }
}
