package com.example.dronedelivery.api.dto;

import com.example.dronedelivery.api.dto.common.Coordinates;
import com.example.dronedelivery.domain.DroneStatus;
import com.example.dronedelivery.domain.JobStatus;
import com.example.dronedelivery.domain.JobType;
import com.example.dronedelivery.domain.OrderStatus;

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
            Coordinates location
    ) {}

    public enum NextAction {
        RESERVE_JOB,
        PICKUP,
        DELIVER_OR_FAIL,
        WAIT
    }

    public record HeartbeatResponse(
            DroneResponse drone,
            Assignment assignment,
            NextAction nextAction
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

    public record JobResponse(
            UUID id,
            UUID orderId,
            JobType type,
            JobStatus status,
            Coordinates pickup,
            Coordinates dropoff,
            UUID assignedDroneId,
            UUID excludedDroneId,
            Instant reservedAt,
            Instant startedAt,
            Instant completedAt,
            Instant failedAt,
            Instant createdAt
    ) {
    }

    public record ReserveJobResponse(
            UUID jobId,
            JobStatus status,
            Instant reservedAt
    ) {}
}
