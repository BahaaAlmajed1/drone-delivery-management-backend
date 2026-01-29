package com.example.dronedelivery.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Drone {

    @Id
    @GeneratedValue
    private UUID id;

    private String name;

    @Enumerated(EnumType.STRING)
    private DroneStatus status = DroneStatus.ACTIVE;

    private Double lastLat = 0.0;
    private Double lastLng = 0.0;

    private Instant lastHeartbeatAt;

    private UUID currentJobId;

    public Drone(String name) {
        this.name = name;
    }
}
