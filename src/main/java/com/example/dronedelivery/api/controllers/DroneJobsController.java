package com.example.dronedelivery.api.controllers;

import com.example.dronedelivery.api.dto.DroneDtos;
import com.example.dronedelivery.domain.Job;
import com.example.dronedelivery.domain.JobStatus;
import com.example.dronedelivery.repo.JobRepository;
import com.example.dronedelivery.security.AuthContext;
import com.example.dronedelivery.service.DroneService;
import com.example.dronedelivery.service.ResponseMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/drone/jobs")
@PreAuthorize("hasRole('DRONE')")
public class DroneJobsController {

    private final DroneService droneService;
    private final JobRepository jobRepository;

    public DroneJobsController(DroneService droneService, JobRepository jobRepository) {
        this.droneService = droneService;
        this.jobRepository = jobRepository;
    }

    private UUID droneId() {
        return AuthContext.actorId();
    }

    @GetMapping("/open")
    public List<DroneDtos.JobResponse> listOpenJobs() {
        return jobRepository.findByStatusOrderByCreatedAtAsc(JobStatus.OPEN)
                .stream()
                .map(ResponseMapper::toDto)
                .toList();
    }

    @PostMapping("/{jobId}/reserve")
    public DroneDtos.ReserveJobResponse reserve(@PathVariable UUID jobId) {
        UUID droneId = droneId();
        Job job = droneService.reserveJob(droneId, jobId);
        return new DroneDtos.ReserveJobResponse(job.getId(), job.getStatus(), job.getReservedAt());
    }

    @PostMapping("/{jobId}/pickup")
    public DroneDtos.JobResponse pickup(@PathVariable UUID jobId) {
        UUID droneId = droneId();
        return ResponseMapper.toDto(droneService.pickupJob(droneId, jobId));
    }

    @PostMapping("/{jobId}/complete")
    public DroneDtos.JobResponse complete(@PathVariable UUID jobId) {
        UUID droneId = droneId();
        return ResponseMapper.toDto(droneService.completeJob(droneId, jobId));
    }

    @PostMapping("/{jobId}/fail")
    public DroneDtos.JobResponse fail(@PathVariable UUID jobId) {
        UUID droneId = droneId();
        return ResponseMapper.toDto(droneService.failJob(droneId, jobId));
    }
}
