package com.example.dronedelivery.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "drones", uniqueConstraints = {
        @UniqueConstraint(name = "uk_drones_name", columnNames = {"name"})
})
public class Drone {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DroneStatus status = DroneStatus.ACTIVE;

    private Double lastLat;
    private Double lastLng;

    private Instant lastHeartbeatAt;

    private UUID currentJobId;

    protected Drone() {}

    public Drone(String name) {
        this.name = name;
        this.status = DroneStatus.ACTIVE;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }

    public DroneStatus getStatus() { return status; }
    public void setStatus(DroneStatus status) { this.status = status; }

    public Double getLastLat() { return lastLat; }
    public void setLastLat(Double lastLat) { this.lastLat = lastLat; }

    public Double getLastLng() { return lastLng; }
    public void setLastLng(Double lastLng) { this.lastLng = lastLng; }

    public Instant getLastHeartbeatAt() { return lastHeartbeatAt; }
    public void setLastHeartbeatAt(Instant lastHeartbeatAt) { this.lastHeartbeatAt = lastHeartbeatAt; }

    public UUID getCurrentJobId() { return currentJobId; }
    public void setCurrentJobId(UUID currentJobId) { this.currentJobId = currentJobId; }
}
