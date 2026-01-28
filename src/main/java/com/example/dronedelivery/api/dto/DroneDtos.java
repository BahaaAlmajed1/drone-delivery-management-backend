package com.example.dronedelivery.api.dto;

import com.example.dronedelivery.domain.DroneStatus;
import com.example.dronedelivery.domain.JobStatus;
import com.example.dronedelivery.domain.JobType;
import com.example.dronedelivery.domain.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public class DroneDtos {

    public record DroneResponse(
            UUID id,
            String name,
            DroneStatus status,
            CommonDtos.LatLng lastLocation,
            Instant lastHeartbeatAt,
            UUID currentJobId
    ) {}

    public record HeartbeatRequest(
            @NotNull @Valid CommonDtos.LatLng location
    ) {}

    public record HeartbeatResponse(
            DroneResponse drone,
            Assignment assignment,
            String nextAction
    ) {}

    public record Assignment(
            UUID jobId,
            JobStatus jobStatus,
            JobType jobType,
            CommonDtos.LatLng pickup,
            CommonDtos.LatLng dropoff,
            UUID orderId,
            OrderStatus orderStatus
    ) {}

    public record ReserveJobResponse(
            UUID jobId,
            JobStatus status,
            Instant reservedAt
    ) {}
}
