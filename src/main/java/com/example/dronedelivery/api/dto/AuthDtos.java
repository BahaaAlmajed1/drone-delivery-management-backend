package com.example.dronedelivery.api.dto;

import com.example.dronedelivery.security.AuthRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Assessment: JWT handed out by an endpoint that takes a name (for security purposes, pretend that this API will be behind an allow list)
 * and a type of user (admin, enduser, or drone).
 */
public class AuthDtos {

    public record TokenRequest(
            @NotBlank String name,
            @NotNull AuthRole userType
    ) {}

    public record TokenResponse(
            String tokenType,
            String accessToken
    ) {}
}
