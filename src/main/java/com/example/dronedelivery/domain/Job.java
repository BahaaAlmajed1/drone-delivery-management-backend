package com.example.dronedelivery.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Job {

    @Id
    @GeneratedValue
    private UUID id;

    @Version
    private long version;

    private UUID orderId;

    @Enumerated(EnumType.STRING)
    private JobType type;

    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.OPEN;

    private double pickupLat;

    private double pickupLng;

    private double dropoffLat;

    private double dropoffLng;

    private UUID assignedDroneId;

    /**
     * For handoff jobs, ensure the original drone can never reserve it even if its fixed later
     */
    private UUID excludedDroneId;

    private Instant reservedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant failedAt;

    private Instant createdAt = Instant.now();

    public Job(UUID orderId, JobType type, double pickupLat, double pickupLng, double dropoffLat, double dropoffLng, UUID excludedDroneId) {
        this.orderId = orderId;
        this.type = type;
        this.pickupLat = pickupLat;
        this.pickupLng = pickupLng;
        this.dropoffLat = dropoffLat;
        this.dropoffLng = dropoffLng;
        this.excludedDroneId = excludedDroneId;
        this.status = JobStatus.OPEN;
        this.createdAt = Instant.now();
    }

    public void setPickup(double lat, double lng) {
        this.pickupLat = lat;
        this.pickupLng = lng;
    }

    public void setDropoff(double lat, double lng) {
        this.dropoffLat = lat;
        this.dropoffLng = lng;
    }
}
