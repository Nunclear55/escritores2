package com.nunclear.escritores.security;

import com.nunclear.escritores.entity.AppUser;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Integer id;
    private final String username;
    private final String password;
    private final String accessLevel;
    private final String accountState;

    public CustomUserDetails(AppUser user) {
        this.id = user.getId();
        this.username = user.getLoginName();
        this.password = user.getPasswordHash();
        this.accessLevel = user.getAccessLevel().name();
        this.accountState = user.getAccountState().name();
    }

    @Override
    public Collection<SimpleGrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + accessLevel.toUpperCase()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !"banned".equals(accountState) && !"suspended".equals(accountState);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !"banned".equals(accountState);
    }
}