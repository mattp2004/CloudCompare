package org.cloudcompare.backend.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.cloudcompare.backend.db.util.Rank;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record JwtUser(UUID userId, UserDetails delegate, Rank rank) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rank.name().toUpperCase()));
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return delegate.getPassword();
    }

    @Override
    @JsonIgnore
    public String getUsername() {
        return delegate.getUsername();
    }
//    @JsonIgnore
//    public UserDetails delegate() { return delegate; }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }

    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
