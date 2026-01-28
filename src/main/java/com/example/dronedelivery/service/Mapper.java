package com.example.dronedelivery.service;

import com.example.dronedelivery.api.dto.CommonDtos;
import com.example.dronedelivery.api.dto.DroneDtos;
import com.example.dronedelivery.api.dto.JobDtos;
import com.example.dronedelivery.api.dto.OrderDtos;
import com.example.dronedelivery.domain.*;

public final class Mapper {

    private Mapper() {}

    public static CommonDtos.LatLng latLng(double lat, double lng) {
        return new CommonDtos.LatLng(lat, lng);
    }

    public static CommonDtos.LatLng latLng(Double lat, Double lng) {
        if (lat == null || lng == null) return null;
        return new CommonDtos.LatLng(lat, lng);
    }

    public static DroneDtos.DroneResponse toDto(Drone d) {
        return new DroneDtos.DroneResponse(
                d.getId(),
                d.getName(),
                d.getStatus(),
                latLng(d.getLastLat(), d.getLastLng()),
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
                latLng(j.getPickupLat(), j.getPickupLng()),
                latLng(j.getDropoffLat(), j.getDropoffLng()),
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
                latLng(o.getOriginLat(), o.getOriginLng()),
                latLng(o.getDestinationLat(), o.getDestinationLng()),
                o.getStatus(),
                o.getCurrentJobId(),
                o.getCreatedAt(),
                progress
        );
    }
}
