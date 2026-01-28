package com.example.dronedelivery.api.dto;

import com.example.dronedelivery.api.dto.common.Coordinates;
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
            Coordinates lastLocation,
            Instant lastHeartbeatAt,
            UUID currentJobId
    ) {}

    public record HeartbeatRequest(
            @NotNull @Valid Coordinates location
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
            Coordinates pickup,
            Coordinates dropoff,
            UUID orderId,
            OrderStatus orderStatus
    ) {}

    public record ReserveJobResponse(
            UUID jobId,
            JobStatus status,
            Instant reservedAt
    ) {}
}
