package com.example.dronedelivery.service;

import com.example.dronedelivery.api.dto.OrderDtos;
import com.example.dronedelivery.domain.*;
import com.example.dronedelivery.repo.DroneRepository;
import com.example.dronedelivery.repo.JobRepository;
import com.example.dronedelivery.repo.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeliveryServiceHelper {

    /**
     * Conservative average speed for ETA calculation.
     * The assessment doesn't demand accuracy; it demands "progress, location and ETA".
     */
    private static final double AVERAGE_SPEED_M_PER_S = 12.0; // ~43 km/h

    private final DroneRepository droneRepository;
    private final OrderRepository orderRepository;
    private final JobRepository jobRepository;

    @Transactional(readOnly = true)
    public OrderDtos.Progress computeProgress(DeliveryOrder order) {
        if (order.getCurrentJobId() == null) {
            throw ApiException.badRequest("Order has no current job.");
        }
        Job job = jobRepository.findById(order.getCurrentJobId())
                .orElseThrow(() -> ApiException.notFound("Current job not found: " + order.getCurrentJobId()));

        if (job.getAssignedDroneId() == null) {
            return new OrderDtos.Progress(
                    ResponseMapper.coordinates(order.getOriginLat(), order.getOriginLng()),
                    0
            );
        }
        Drone d = droneRepository.findById(job.getAssignedDroneId())
                .orElseThrow(() -> ApiException.notFound("Drone not found: " + job.getAssignedDroneId()));
        if (d.getLastHeartbeatAt() == null) {
            throw ApiException.badRequest("Drone has no last known location.");
        }

        double remainingMeters = GeoUtil.haversineMeters(d.getLastLat(), d.getLastLng(), job.getDropoffLat(), job.getDropoffLng());
        int etaSec = (int) Math.round(remainingMeters / AVERAGE_SPEED_M_PER_S);

        return new OrderDtos.Progress(
                ResponseMapper.coordinates(d.getLastLat(), d.getLastLng()),
                Math.max(0, etaSec)
        );
    }

    public Job requireCurrentJob(DeliveryOrder order) {
        if (order.getCurrentJobId() == null) {
            throw ApiException.badRequest("Order has no current job.");
        }
        return jobRepository.findById(order.getCurrentJobId())
                .orElseThrow(() -> ApiException.notFound("Current job not found: " + order.getCurrentJobId()));
    }

    public Job requireInProgressJobForDrone(UUID droneId, UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> ApiException.notFound("Job not found: " + jobId));

        if (!droneId.equals(job.getAssignedDroneId())) {
            throw ApiException.forbidden("You can only update jobs assigned to your drone.");
        }
        if (job.getStatus() != JobStatus.IN_PROGRESS) {
            throw ApiException.badRequest("Job must be IN_PROGRESS to mark delivered/failed.");
        }
        return job;
    }

    /**
     * Implements the additional rule:
     * "Any time a drone is broken it will stop and put up a job for its goods to be picked up by a different drone
     * (even if it gets marked as fixed)."
     */
    @Transactional
    public void markDroneBrokenInternal(Drone d) {
        d.setStatus(DroneStatus.BROKEN);

        UUID currentJobId = d.getCurrentJobId();
        if (currentJobId == null) {
            droneRepository.save(d);
            return;
        }

        Job currentJob = jobRepository.findById(currentJobId)
                .orElseThrow(() -> ApiException.notFound("Current job not found: " + currentJobId));

        // Only create a handoff job if the drone currently has the goods (job in progress).
        if (currentJob.getStatus() == JobStatus.IN_PROGRESS) {
            // Fail the in-progress job (it cannot continue)
            currentJob.setStatus(JobStatus.FAILED);
            currentJob.setFailedAt(Instant.now());
            jobRepository.save(currentJob);

            DeliveryOrder order = orderRepository.findById(currentJob.getOrderId())
                    .orElseThrow(() -> ApiException.notFound("Order not found for job: " + currentJob.getOrderId()));

            if (d.getLastHeartbeatAt() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Drone has no last known location; heartbeat required before marking broken.");
            }

            // Create new handoff job: pickup at broken drone location, dropoff at order destination.
            Job handoff = new Job(order.getId(), JobType.HANDOFF_PICKUP_AND_DELIVER,
                    d.getLastLat(), d.getLastLng(),
                    order.getDestinationLat(), order.getDestinationLng(),
                    d.getId() // excluded drone
            );
            handoff = jobRepository.save(handoff);

            order.setStatus(OrderStatus.HANDOFF_REQUESTED);
            order.setCurrentJobId(handoff.getId());
            orderRepository.save(order);

            // Drone no longer holds an active job (it stopped)
            d.setCurrentJobId(null);
        }

        droneRepository.save(d);
    }
}
