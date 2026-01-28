package com.example.dronedelivery.api;

import com.example.dronedelivery.api.dto.DroneDtos;
import com.example.dronedelivery.api.dto.JobDtos;
import com.example.dronedelivery.domain.Job;
import com.example.dronedelivery.domain.JobStatus;
import com.example.dronedelivery.repo.JobRepository;
import com.example.dronedelivery.repo.OrderRepository;
import com.example.dronedelivery.security.AuthContext;
import com.example.dronedelivery.service.ApiException;
import com.example.dronedelivery.service.DroneService;
import com.example.dronedelivery.service.ResponseMapper;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/drone")
@PreAuthorize("hasRole('DRONE')")
public class DroneController {

    private final DroneService droneService;
    private final JobRepository jobRepository;
    private final OrderRepository orderRepository;

    public DroneController(DroneService droneService, JobRepository jobRepository, OrderRepository orderRepository) {
        this.droneService = droneService;
        this.jobRepository = jobRepository;
        this.orderRepository = orderRepository;
    }

    private UUID droneId() {
        UUID id = AuthContext.actorIdOrNull();
        if (id == null) throw ApiException.forbidden("Drone token missing actorId.");
        return id;
    }

    @PostMapping("/self/heartbeat")
    public DroneDtos.HeartbeatResponse heartbeat(@Valid @RequestBody DroneDtos.HeartbeatRequest req) {
        UUID droneId = droneId();
        var d = droneService.heartbeat(droneId, req.location().lat(), req.location().lng());

        Job job = droneService.getCurrentJobForDrone(droneId);
        DroneDtos.Assignment assignment = null;

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
        }

        String nextAction = (job == null) ? "RESERVE_JOB" :
                (job.getStatus() == JobStatus.RESERVED ? "PICKUP" :
                        (job.getStatus() == JobStatus.IN_PROGRESS ? "DELIVER_OR_FAIL" : "WAIT"));

        return new DroneDtos.HeartbeatResponse(ResponseMapper.toDto(d), assignment, nextAction);
    }

    @GetMapping("/jobs/open")
    public List<JobDtos.JobResponse> listOpenJobs() {
        return jobRepository.findByStatusOrderByCreatedAtAsc(JobStatus.OPEN)
                .stream()
                .map(ResponseMapper::toDto)
                .toList();
    }

    @PostMapping("/jobs/{jobId}/reserve")
    public DroneDtos.ReserveJobResponse reserve(@PathVariable UUID jobId) {
        UUID droneId = droneId();
        Job job = droneService.reserveJob(droneId, jobId);
        return new DroneDtos.ReserveJobResponse(job.getId(), job.getStatus(), job.getReservedAt());
    }

    @PostMapping("/jobs/{jobId}/pickup")
    public JobDtos.JobResponse pickup(@PathVariable UUID jobId) {
        UUID droneId = droneId();
        return ResponseMapper.toDto(droneService.pickupJob(droneId, jobId));
    }

    @PostMapping("/jobs/{jobId}/complete")
    public JobDtos.JobResponse complete(@PathVariable UUID jobId) {
        UUID droneId = droneId();
        return ResponseMapper.toDto(droneService.completeJob(droneId, jobId));
    }

    @PostMapping("/jobs/{jobId}/fail")
    public JobDtos.JobResponse fail(@PathVariable UUID jobId) {
        UUID droneId = droneId();
        return ResponseMapper.toDto(droneService.failJob(droneId, jobId));
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
                order == null ? null : order.getStatus()
        );
    }
}
