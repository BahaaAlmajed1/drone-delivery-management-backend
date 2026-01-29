package com.example.dronedelivery.api.controllers;

import com.example.dronedelivery.api.dto.DroneDtos;
import com.example.dronedelivery.api.dto.OrderDtos;
import com.example.dronedelivery.domain.DroneStatus;
import com.example.dronedelivery.repo.JobRepository;
import com.example.dronedelivery.repo.OrderRepository;
import com.example.dronedelivery.service.AdminService;
import com.example.dronedelivery.service.ApiException;
import com.example.dronedelivery.service.ResponseMapper;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final OrderRepository orderRepository;
    private final JobRepository jobRepository;

    public AdminController(AdminService adminService, OrderRepository orderRepository, JobRepository jobRepository) {
        this.adminService = adminService;
        this.orderRepository = orderRepository;
        this.jobRepository = jobRepository;
    }

    @GetMapping("/orders")
    public List<OrderDtos.OrderResponse> listAllOrders() {
        return adminService.listAllOrders()
                .stream()
                .map(o -> ResponseMapper.toDto(o, adminService.computeProgress(o)))
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
                .map(o -> ResponseMapper.toDto(o, adminService.computeProgress(o)))
                .toList();
    }

    @PostMapping("/orders/{orderId}")
    public OrderDtos.OrderResponse updateOrder(@PathVariable UUID orderId, @Valid @RequestBody OrderDtos.AdminUpdateOrderRequest req) {
        Double oLat = req.origin().lat();
        Double oLng = req.origin().lng();
        Double dLat = req.destination().lat();
        Double dLng = req.destination().lng();

        var order = adminService.adminUpdateOrder(orderId, oLat, oLng, dLat, dLng);
        return ResponseMapper.toDto(order, adminService.computeProgress(order));
    }

    @GetMapping("/drones")
    public List<DroneDtos.DroneResponse> listDrones() {
        return adminService.listDrones().stream().map(ResponseMapper::toDto).toList();
    }

    public record SetDroneStatusRequest(DroneStatus status) {}

    @PostMapping("/drones/{droneId}/status")
    public DroneDtos.DroneResponse setDroneStatus(@PathVariable UUID droneId, @RequestBody SetDroneStatusRequest req) {
        if (req == null || req.status() == null) throw ApiException.badRequest("status is required.");
        return ResponseMapper.toDto(adminService.adminSetDroneStatus(droneId, req.status()));
    }

    @GetMapping("/jobs")
    public List<DroneDtos.JobResponse> listAllJobs() {
        return jobRepository.findAll()
                .stream()
                .map(ResponseMapper::toDto)
                .toList();
    }
}
