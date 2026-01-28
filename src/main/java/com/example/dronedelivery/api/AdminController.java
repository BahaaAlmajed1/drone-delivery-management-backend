package com.example.dronedelivery.api;

import com.example.dronedelivery.api.dto.DroneDtos;
import com.example.dronedelivery.api.dto.OrderDtos;
import com.example.dronedelivery.domain.DroneStatus;
import com.example.dronedelivery.repo.OrderRepository;
import com.example.dronedelivery.service.ApiException;
import com.example.dronedelivery.service.DeliveryService;
import com.example.dronedelivery.service.Mapper;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final DeliveryService deliveryService;
    private final OrderRepository orderRepository;

    public AdminController(DeliveryService deliveryService, OrderRepository orderRepository) {
        this.deliveryService = deliveryService;
        this.orderRepository = orderRepository;
    }

    @GetMapping("/orders")
    public List<OrderDtos.OrderResponse> listAllOrders() {
        return deliveryService.listAllOrders()
                .stream()
                .map(o -> Mapper.toDto(o, deliveryService.computeProgress(o)))
                .toList();
    }

    public record BulkOrdersRequest(List<UUID> orderIds) {}

    @PostMapping("/orders/bulk")
    public List<OrderDtos.OrderResponse> bulk(@RequestBody BulkOrdersRequest req) {
        if (req == null || req.orderIds() == null || req.orderIds().isEmpty()) {
            throw ApiException.badRequest("orderIds is required.");
        }
        return orderRepository.findAllById(req.orderIds())
                .stream()
                .map(o -> Mapper.toDto(o, deliveryService.computeProgress(o)))
                .toList();
    }

    @PatchMapping("/orders/{orderId}")
    public OrderDtos.OrderResponse updateOrder(@PathVariable UUID orderId, @Valid @RequestBody OrderDtos.AdminUpdateOrderRequest req) {
        Double oLat = req.origin() == null ? null : req.origin().lat();
        Double oLng = req.origin() == null ? null : req.origin().lng();
        Double dLat = req.destination() == null ? null : req.destination().lat();
        Double dLng = req.destination() == null ? null : req.destination().lng();

        var order = deliveryService.adminUpdateOrder(orderId, oLat, oLng, dLat, dLng);
        return Mapper.toDto(order, deliveryService.computeProgress(order));
    }

    @GetMapping("/drones")
    public List<DroneDtos.DroneResponse> listDrones() {
        return deliveryService.listDrones().stream().map(Mapper::toDto).toList();
    }

    public record SetDroneStatusRequest(DroneStatus status) {}

    @PostMapping("/drones/{droneId}/status")
    public DroneDtos.DroneResponse setDroneStatus(@PathVariable UUID droneId, @RequestBody SetDroneStatusRequest req) {
        if (req == null || req.status() == null) throw ApiException.badRequest("status is required.");
        return Mapper.toDto(deliveryService.adminSetDroneStatus(droneId, req.status()));
    }
}
