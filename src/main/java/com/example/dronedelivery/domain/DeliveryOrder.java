package com.example.dronedelivery.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class DeliveryOrder {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID createdByEndUserId;

    @Column(nullable = false)
    private double originLat;

    @Column(nullable = false)
    private double originLng;

    @Column(nullable = false)
    private double destinationLat;

    @Column(nullable = false)
    private double destinationLng;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.SUBMITTED;

    private UUID currentJobId;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected DeliveryOrder() {}

    public DeliveryOrder(UUID createdByEndUserId, double originLat, double originLng, double destinationLat, double destinationLng) {
        this.createdByEndUserId = createdByEndUserId;
        this.originLat = originLat;
        this.originLng = originLng;
        this.destinationLat = destinationLat;
        this.destinationLng = destinationLng;
        this.status = OrderStatus.SUBMITTED;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getCreatedByEndUserId() { return createdByEndUserId; }

    public double getOriginLat() { return originLat; }
    public double getOriginLng() { return originLng; }
    public double getDestinationLat() { return destinationLat; }
    public double getDestinationLng() { return destinationLng; }

    public void setOrigin(double lat, double lng) { this.originLat = lat; this.originLng = lng; }
    public void setDestination(double lat, double lng) { this.destinationLat = lat; this.destinationLng = lng; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public UUID getCurrentJobId() { return currentJobId; }
    public void setCurrentJobId(UUID currentJobId) { this.currentJobId = currentJobId; }

    public Instant getCreatedAt() { return createdAt; }
}
