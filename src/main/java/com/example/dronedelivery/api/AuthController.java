package com.example.dronedelivery.api;

import com.example.dronedelivery.api.dto.AuthDtos;
import com.example.dronedelivery.security.AuthRole;
import com.example.dronedelivery.security.JwtService;
import com.example.dronedelivery.service.IdentityService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtService jwtService;
    private final IdentityService identityService;

    public AuthController(JwtService jwtService, IdentityService identityService) {
        this.jwtService = jwtService;
        this.identityService = identityService;
    }

    @PostMapping("/token")
    public AuthDtos.TokenResponse token(@Valid @RequestBody AuthDtos.TokenRequest req) {
        UUID actorId = null;

        // The assessment says this endpoint is allowlisted; we keep it simple, self-signed JWT.
        if (req.userType() == AuthRole.DRONE) {
            actorId = identityService.getOrCreateDrone(req.name()).getId();
        } else if (req.userType() == AuthRole.ENDUSER) {
            actorId = identityService.getOrCreateEndUser(req.name()).getId();
        }

        String jwt = jwtService.issueToken(req.name(), req.userType(), actorId);
        return new AuthDtos.TokenResponse("Bearer", jwt);
    }
}
