package com.moni.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final String userId;
    private final String role;
    private final List<? extends GrantedAuthority> authorities;

    public CustomUserDetails(String userId, String role, List<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.role = role;
        this.authorities = authorities;
    }

    public String getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return userId;
    }
}
