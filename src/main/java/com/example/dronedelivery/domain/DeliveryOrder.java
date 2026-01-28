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
public class DeliveryOrder {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID createdByEndUserId;

    private double originLat;

    private double originLng;

    private double destinationLat;

    private double destinationLng;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.SUBMITTED;

    private UUID currentJobId;

    private Instant createdAt = Instant.now();

    public DeliveryOrder(UUID createdByEndUserId, double originLat, double originLng, double destinationLat, double destinationLng) {
        this.createdByEndUserId = createdByEndUserId;
        this.originLat = originLat;
        this.originLng = originLng;
        this.destinationLat = destinationLat;
        this.destinationLng = destinationLng;
        this.status = OrderStatus.SUBMITTED;
        this.createdAt = Instant.now();
    }

    public void setOrigin(double lat, double lng) {
        this.originLat = lat;
        this.originLng = lng;
    }

    public void setDestination(double lat, double lng) {
        this.destinationLat = lat;
        this.destinationLng = lng;
    }
}
