package com.example.dronedelivery.api.dto;

import com.example.dronedelivery.domain.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public class OrderDtos {

    public record SubmitOrderRequest(
            @NotNull @Valid CommonDtos.LatLng origin,
            @NotNull @Valid CommonDtos.LatLng destination
    ) {}

    public record OrderResponse(
            UUID id,
            UUID createdByEndUserId,
            CommonDtos.LatLng origin,
            CommonDtos.LatLng destination,
            OrderStatus status,
            UUID currentJobId,
            Instant createdAt,
            Progress progress
    ) {}

    public record Progress(
            CommonDtos.LatLng currentLocation,
            Integer etaSecondsApprox
    ) {}

    public record AdminUpdateOrderRequest(
            @Valid CommonDtos.LatLng origin,
            @Valid CommonDtos.LatLng destination
    ) {}
}
