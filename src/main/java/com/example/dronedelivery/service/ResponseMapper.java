package com.example.dronedelivery.service;

import com.example.dronedelivery.api.dto.DroneDtos;
import com.example.dronedelivery.api.dto.JobDtos;
import com.example.dronedelivery.api.dto.OrderDtos;
import com.example.dronedelivery.api.dto.common.Coordinates;
import com.example.dronedelivery.domain.DeliveryOrder;
import com.example.dronedelivery.domain.Drone;
import com.example.dronedelivery.domain.Job;

public final class ResponseMapper {

    private ResponseMapper() {
    }

    public static Coordinates coordinates(double lat, double lng) {
        return new Coordinates(lat, lng);
    }

    public static Coordinates coordinates(Double lat, Double lng) {
        if (lat == null || lng == null) return null;
        return new Coordinates(lat, lng);
    }

    public static DroneDtos.DroneResponse toDto(Drone d) {
        return new DroneDtos.DroneResponse(
                d.getId(),
                d.getName(),
                d.getStatus(),
                coordinates(d.getLastLat(), d.getLastLng()),
                d.getLastHeartbeatAt(),
                d.getCurrentJobId()
        );
    }

    public static JobDtos.JobResponse toDto(Job j) {
        return new JobDtos.JobResponse(
                j.getId(),
                j.getOrderId(),
                j.getType(),
                j.getStatus(),
                coordinates(j.getPickupLat(), j.getPickupLng()),
                coordinates(j.getDropoffLat(), j.getDropoffLng()),
                j.getAssignedDroneId(),
                j.getExcludedDroneId(),
                j.getReservedAt(),
                j.getStartedAt(),
                j.getCompletedAt(),
                j.getFailedAt(),
                j.getCreatedAt()
        );
    }

    public static OrderDtos.OrderResponse toDto(DeliveryOrder o, OrderDtos.Progress progress) {
        return new OrderDtos.OrderResponse(
                o.getId(),
                o.getCreatedByEndUserId(),
                coordinates(o.getOriginLat(), o.getOriginLng()),
                coordinates(o.getDestinationLat(), o.getDestinationLng()),
                o.getStatus(),
                o.getCurrentJobId(),
                o.getCreatedAt(),
                progress
        );
    }
}
