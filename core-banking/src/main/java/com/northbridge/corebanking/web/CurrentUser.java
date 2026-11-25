package com.northbridge.corebanking.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    private final JdbcTemplate jdbc;

    @Autowired
    public CurrentUser(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String username() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "anonymous" : auth.getName();
    }

    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (a.getAuthority().equals("ROLE_" + role)) {
                return true;
            }
        }
        return false;
    }

    public boolean isCustomer() {
        return hasRole("CUSTOMER");
    }

    /** CIF linked to the signed-in online-banking user; null for staff users. */
    public String cif() {
        try {
            return jdbc.queryForObject("SELECT cif FROM app_user WHERE username = ?", String.class, username());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
