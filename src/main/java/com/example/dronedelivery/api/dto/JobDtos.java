package com.example.dronedelivery.api.dto;

import com.example.dronedelivery.domain.JobStatus;
import com.example.dronedelivery.domain.JobType;

import java.time.Instant;
import java.util.UUID;

public class JobDtos {

    public record JobResponse(
            UUID id,
            UUID orderId,
            JobType type,
            JobStatus status,
            CommonDtos.LatLng pickup,
            CommonDtos.LatLng dropoff,
            UUID assignedDroneId,
            UUID excludedDroneId,
            Instant reservedAt,
            Instant startedAt,
            Instant completedAt,
            Instant failedAt,
            Instant createdAt
    ) {}
}
