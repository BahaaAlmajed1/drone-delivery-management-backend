package com.example.dronedelivery.security;

import java.util.UUID;

public record AuthPrincipal(String name, AuthRole role, UUID actorId) {}
