package com.babaai.core.security;

import com.babaai.core.domain.User;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;

public class AuthenticatedUser implements org.springframework.security.core.userdetails.UserDetails {

    private final User user;
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthenticatedUser(User user, Collection<? extends GrantedAuthority> authorities) {
        this.user = user;
        this.authorities = authorities;
    }

    public User getUser() {
        return user;
    }

    public UUID getId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getHashedPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        if (!user.isAccountNonLocked()) {
            return false;
        }
        Instant lockedUntil = user.getLockedUntil();
        return lockedUntil == null || lockedUntil.isBefore(Instant.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}
