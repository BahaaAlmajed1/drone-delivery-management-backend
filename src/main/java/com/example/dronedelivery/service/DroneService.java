package com.example.dronedelivery.service;

import com.example.dronedelivery.domain.*;
import com.example.dronedelivery.repo.DroneRepository;
import com.example.dronedelivery.repo.JobRepository;
import com.example.dronedelivery.repo.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DroneService {

    private final DroneRepository droneRepository;
    private final OrderRepository orderRepository;
    private final JobRepository jobRepository;
    private final DeliveryServiceHelper helper;

    @Transactional
    public Drone heartbeat(UUID droneId, double lat, double lng) {
        Drone d = droneRepository.findById(droneId)
                .orElseThrow(() -> ApiException.notFound("Drone not found: " + droneId));
        d.setLastLat(lat);
        d.setLastLng(lng);
        d.setLastHeartbeatAt(Instant.now());
        return droneRepository.save(d);
    }

    @Transactional
    public Job reserveJob(UUID droneId, UUID jobId) {
        Drone d = droneRepository.findById(droneId)
                .orElseThrow(() -> ApiException.notFound("Drone not found: " + droneId));

        if (d.getStatus() == DroneStatus.BROKEN) {
            throw ApiException.badRequest("Broken drones cannot reserve jobs.");
        }

        try {
            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> ApiException.notFound("Job not found: " + jobId));

            if (job.getStatus() != JobStatus.OPEN) {
                throw ApiException.conflict("Job is not open for reservation.");
            }
            if (job.getExcludedDroneId() != null && job.getExcludedDroneId().equals(droneId)) {
                throw ApiException.forbidden("This job must be picked up by a different drone.");
            }

            job.setAssignedDroneId(droneId);
            job.setStatus(JobStatus.RESERVED);
            job.setReservedAt(Instant.now());

            Job saved = jobRepository.save(job);

            d.setCurrentJobId(saved.getId());
            droneRepository.save(d);

            return saved;
        } catch (OptimisticLockingFailureException e) {
            throw ApiException.conflict("Reservation conflict. Another drone reserved this job first.");
        }
    }

    @Transactional
    public Job pickupJob(UUID droneId, UUID jobId) {
        Drone d = droneRepository.findById(droneId)
                .orElseThrow(() -> ApiException.notFound("Drone not found: " + droneId));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> ApiException.notFound("Job not found: " + jobId));

        if (!droneId.equals(job.getAssignedDroneId())) {
            throw ApiException.forbidden("You can only pick up a job reserved for your drone.");
        }
        if (job.getStatus() != JobStatus.RESERVED) {
            throw ApiException.badRequest("Job must be RESERVED to start pickup.");
        }

        job.setStatus(JobStatus.IN_PROGRESS);
        job.setStartedAt(Instant.now());
        jobRepository.save(job);

        DeliveryOrder order = orderRepository.findById(job.getOrderId())
                .orElseThrow(() -> ApiException.notFound("Order not found for job: " + job.getOrderId()));
        order.setStatus(OrderStatus.IN_DELIVERY);
        orderRepository.save(order);

        d.setCurrentJobId(job.getId());
        droneRepository.save(d);

        return job;
    }

    @Transactional
    public Job completeJob(UUID droneId, UUID jobId) {
        Job job = helper.requireInProgressJobForDrone(droneId, jobId);

        job.setStatus(JobStatus.COMPLETED);
        job.setCompletedAt(Instant.now());
        jobRepository.save(job);

        DeliveryOrder order = orderRepository.findById(job.getOrderId())
                .orElseThrow(() -> ApiException.notFound("Order not found for job: " + job.getOrderId()));
        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        Drone d = droneRepository.findById(droneId).orElseThrow();
        d.setCurrentJobId(null);
        droneRepository.save(d);

        return job;
    }

    @Transactional
    public Job failJob(UUID droneId, UUID jobId) {
        Job job = helper.requireInProgressJobForDrone(droneId, jobId);

        job.setStatus(JobStatus.FAILED);
        job.setFailedAt(Instant.now());
        jobRepository.save(job);

        DeliveryOrder order = orderRepository.findById(job.getOrderId())
                .orElseThrow(() -> ApiException.notFound("Order not found for job: " + job.getOrderId()));
        order.setStatus(OrderStatus.FAILED);
        orderRepository.save(order);

        Drone d = droneRepository.findById(droneId).orElseThrow();
        d.setCurrentJobId(null);
        droneRepository.save(d);

        return job;
    }

    @Transactional
    public Drone droneMarkBroken(UUID droneId) {
        Drone d = droneRepository.findById(droneId)
                .orElseThrow(() -> ApiException.notFound("Drone not found: " + droneId));
        helper.markDroneBrokenInternal(d);
        return d;
    }

    @Transactional
    public Drone droneMarkFixed(UUID droneId) {
        Drone d = droneRepository.findById(droneId)
                .orElseThrow(() -> ApiException.notFound("Drone not found: " + droneId));
        d.setStatus(DroneStatus.FIXED);
        return droneRepository.save(d);
    }

    @Transactional(readOnly = true)
    public Job getCurrentJobForDrone(UUID droneId) {
        Drone d = droneRepository.findById(droneId)
                .orElseThrow(() -> ApiException.notFound("Drone not found: " + droneId));
        if (d.getCurrentJobId() == null) {
            throw ApiException.badRequest("Drone has no current job.");
        }
        return jobRepository.findById(d.getCurrentJobId())
                .orElseThrow(() -> ApiException.notFound("Current job not found: " + d.getCurrentJobId()));
    }
}
