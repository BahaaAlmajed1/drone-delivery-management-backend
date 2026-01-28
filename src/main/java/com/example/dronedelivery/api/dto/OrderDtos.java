package com.example.dronedelivery.api.dto;

import com.example.dronedelivery.api.dto.common.Coordinates;
import com.example.dronedelivery.domain.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public class OrderDtos {

    public record SubmitOrderRequest(
            @NotNull @Valid Coordinates origin,
            @NotNull @Valid Coordinates destination
    ) {}

    public record OrderResponse(
            UUID id,
            UUID createdByEndUserId,
            Coordinates origin,
            Coordinates destination,
            OrderStatus status,
            UUID currentJobId,
            Instant createdAt,
            Progress progress
    ) {}

    public record Progress(
            Coordinates currentLocation,
            Integer etaSecondsApprox
    ) {}

    public record AdminUpdateOrderRequest(
            @Valid Coordinates origin,
            @Valid Coordinates destination
    ) {}
}
