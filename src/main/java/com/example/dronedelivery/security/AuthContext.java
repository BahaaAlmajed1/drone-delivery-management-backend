package com.example.dronedelivery.security;

import com.example.dronedelivery.service.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class AuthContext {

    private AuthContext() {}

    public static UUID actorId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof AuthPrincipal p)) {
            throw ApiException.forbidden("Authentication is missing an actor id.");
        }
        return p.actorId();
    }
}
