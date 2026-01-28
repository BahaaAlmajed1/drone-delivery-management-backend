package com.example.dronedelivery.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class AuthContext {

    private AuthContext() {}

    public static AuthRole role() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof AuthPrincipal p)) throw new IllegalStateException("No auth principal");
        return p.role();
    }

    public static UUID actorIdOrNull() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof AuthPrincipal p)) return null;
        return p.actorId();
    }

    public static AuthPrincipal principal() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof AuthPrincipal p)) throw new IllegalStateException("No auth principal");
        return p;
    }
}
