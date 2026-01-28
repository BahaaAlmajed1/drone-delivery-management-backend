package com.example.dronedelivery.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs", indexes = {
        @Index(name = "idx_jobs_status", columnList = "status"),
        @Index(name = "idx_jobs_order_id", columnList = "orderId")
})
public class Job {

    @Id
    @GeneratedValue
    private UUID id;

    @Version
    private long version;

    @Column(nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.OPEN;

    @Column(nullable = false)
    private double pickupLat;

    @Column(nullable = false)
    private double pickupLng;

    @Column(nullable = false)
    private double dropoffLat;

    @Column(nullable = false)
    private double dropoffLng;

    private UUID assignedDroneId;

    /**
     * For handoff jobs, ensure the initiating broken drone can never reserve it
     * (satisfies: "picked up by a different drone, even if it gets marked as fixed").
     */
    private UUID excludedDroneId;

    private Instant reservedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant failedAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Job() {}

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

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public JobType getType() { return type; }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public double getPickupLat() { return pickupLat; }
    public double getPickupLng() { return pickupLng; }
    public double getDropoffLat() { return dropoffLat; }
    public double getDropoffLng() { return dropoffLng; }

    public void setPickup(double lat, double lng) { this.pickupLat = lat; this.pickupLng = lng; }
    public void setDropoff(double lat, double lng) { this.dropoffLat = lat; this.dropoffLng = lng; }

    public UUID getAssignedDroneId() { return assignedDroneId; }
    public void setAssignedDroneId(UUID assignedDroneId) { this.assignedDroneId = assignedDroneId; }

    public UUID getExcludedDroneId() { return excludedDroneId; }
    public Instant getReservedAt() { return reservedAt; }
    public void setReservedAt(Instant reservedAt) { this.reservedAt = reservedAt; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getFailedAt() { return failedAt; }
    public void setFailedAt(Instant failedAt) { this.failedAt = failedAt; }

    public Instant getCreatedAt() { return createdAt; }
}
