package com.dealersautocenter.inventory.shared.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * UserDetails extension that carries the tenant identifier for the authenticated principal.
 * GLOBAL_ADMIN users have a {@code null} tenantId, meaning they are not restricted to a single tenant.
 */
public class CustomUserDetails implements UserDetails {

    private final String username;
    private final String password;
    /** Null only for GLOBAL_ADMIN – they are not bound to a specific tenant. */
    private final String tenantId;
    private final List<GrantedAuthority> authorities;

    public CustomUserDetails(String username, String password, String tenantId, String... roles) {
        this.username = username;
        this.password = password;
        this.tenantId = tenantId;
        this.authorities = Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .map(a -> (GrantedAuthority) a)
                .toList();
    }

    /** Returns the tenant this user belongs to, or {@code null} for GLOBAL_ADMIN. */
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
