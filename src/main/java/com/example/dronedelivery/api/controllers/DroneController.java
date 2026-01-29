package com.example.dronedelivery.api.controllers;

import com.example.dronedelivery.api.dto.DroneDtos;
import com.example.dronedelivery.domain.Job;
import com.example.dronedelivery.domain.JobStatus;
import com.example.dronedelivery.repo.OrderRepository;
import com.example.dronedelivery.security.AuthContext;
import com.example.dronedelivery.service.DroneService;
import com.example.dronedelivery.service.ResponseMapper;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/drone")
@PreAuthorize("hasRole('DRONE')")
public class DroneController {

    private final DroneService droneService;
    private final OrderRepository orderRepository;

    public DroneController(DroneService droneService, OrderRepository orderRepository) {
        this.droneService = droneService;
        this.orderRepository = orderRepository;
    }

    private UUID droneId() {
        return AuthContext.actorId();
    }

    @PostMapping("/self/heartbeat")
    public DroneDtos.HeartbeatResponse heartbeat(@Valid @RequestBody DroneDtos.HeartbeatRequest req) {
        UUID droneId = droneId();
        var d = droneService.heartbeat(droneId, req.location().lat(), req.location().lng());

        Job job = droneService.getCurrentJobForDrone(droneId);
        DroneDtos.Assignment assignment = null;
        DroneDtos.NextAction nextAction = DroneDtos.NextAction.RESERVE_JOB;

        if (job != null) {
            var order = orderRepository.findById(job.getOrderId()).orElse(null);
            assignment = new DroneDtos.Assignment(
                    job.getId(),
                    job.getStatus(),
                    job.getType(),
                    ResponseMapper.coordinates(job.getPickupLat(), job.getPickupLng()),
                    ResponseMapper.coordinates(job.getDropoffLat(), job.getDropoffLng()),
                    job.getOrderId(),
                    order == null ? null : order.getStatus()
            );
            if (job.getStatus() == JobStatus.RESERVED) {
                nextAction = DroneDtos.NextAction.PICKUP;
            } else if (job.getStatus() == JobStatus.IN_PROGRESS) {
                nextAction = DroneDtos.NextAction.DELIVER_OR_FAIL;
            } else {
                nextAction = DroneDtos.NextAction.WAIT;
            }
        }


        return new DroneDtos.HeartbeatResponse(ResponseMapper.toDto(d), assignment, nextAction);
    }

    @PostMapping("/self/broken")
    public DroneDtos.DroneResponse markBroken() {
        UUID droneId = droneId();
        return ResponseMapper.toDto(droneService.droneMarkBroken(droneId));
    }

    @PostMapping("/self/fixed")
    public DroneDtos.DroneResponse markFixed() {
        UUID droneId = droneId();
        return ResponseMapper.toDto(droneService.droneMarkFixed(droneId));
    }

    @GetMapping("/self/job")
    public DroneDtos.Assignment currentAssignment() {
        UUID droneId = droneId();
        Job job = droneService.getCurrentJobForDrone(droneId);
        if (job == null) return null;

        var order = orderRepository.findById(job.getOrderId()).orElse(null);
        return new DroneDtos.Assignment(
                job.getId(),
                job.getStatus(),
                job.getType(),
                ResponseMapper.coordinates(job.getPickupLat(), job.getPickupLng()),
                ResponseMapper.coordinates(job.getDropoffLat(), job.getDropoffLng()),
                job.getOrderId(),
                order.getStatus()
        );
    }
}
