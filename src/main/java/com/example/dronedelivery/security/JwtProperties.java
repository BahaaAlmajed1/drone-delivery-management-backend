package com.example.dronedelivery.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt")
@Data
public class JwtProperties {
    /**
     * HMAC secret. Keep it out of git for real projects; provided via env var in docker-compose.
     */
    private String secret = "dev-secret-change-me-dev-secret-change-me";

    /**
     * Token TTL.
     */
    private Duration ttl = Duration.ofHours(8);

    // Lombok generates getters/setters.
}
