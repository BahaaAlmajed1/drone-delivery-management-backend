package com.example.dronedelivery.service;

import com.example.dronedelivery.api.dto.OrderDtos;
import com.example.dronedelivery.domain.*;
import com.example.dronedelivery.repo.DroneRepository;
import com.example.dronedelivery.repo.JobRepository;
import com.example.dronedelivery.repo.OrderRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DeliveryService {

    /**
     * Conservative average speed for ETA calculation.
     * The assessment doesn't demand accuracy; it demands "progress, location and ETA".
     */
    private static final double AVERAGE_SPEED_M_PER_S = 12.0; // ~43 km/h

    private final DroneRepository droneRepository;
    private final OrderRepository orderRepository;
    private final JobRepository jobRepository;

    public DeliveryService(DroneRepository droneRepository, OrderRepository orderRepository, JobRepository jobRepository) {
        this.droneRepository = droneRepository;
        this.orderRepository = orderRepository;
        this.jobRepository = jobRepository;
    }

    // -------- EndUser flows --------

    @Transactional
    public DeliveryOrder submitOrder(UUID endUserId, double originLat, double originLng, double destLat, double destLng) {
        DeliveryOrder order = new DeliveryOrder(endUserId, originLat, originLng, destLat, destLng);
        order = orderRepository.save(order);

        Job job = new Job(order.getId(), JobType.PICKUP_AND_DELIVER, originLat, originLng, destLat, destLng, null);
        job = jobRepository.save(job);

        order.setCurrentJobId(job.getId());
        orderRepository.save(order);

        return order;
    }

    @Transactional
    public DeliveryOrder withdrawOrder(UUID endUserId, UUID orderId) {
        DeliveryOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found: " + orderId));

        if (!order.getCreatedByEndUserId().equals(endUserId)) {
            throw ApiException.forbidden("You can only withdraw your own orders.");
        }
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.FAILED) {
            throw ApiException.badRequest("Order is already finished and cannot be withdrawn.");
        }

        Job currentJob = requireCurrentJob(order);

        // "Withdraw orders that have not yet been picked up."
        // We interpret "picked up" as job IN_PROGRESS.
        if (currentJob.getStatus() == JobStatus.IN_PROGRESS) {
            throw ApiException.badRequest("Order has been picked up; cannot withdraw.");
        }

        order.setStatus(OrderStatus.CANCELED);
        currentJob.setStatus(JobStatus.CANCELED);
        jobRepository.save(currentJob);
        orderRepository.save(order);
        return order;
    }

    @Transactional(readOnly = true)
    public List<DeliveryOrder> listOrdersForEndUser(UUID endUserId) {
        return orderRepository.findByCreatedByEndUserIdOrderByCreatedAtDesc(endUserId);
    }

    @Transactional(readOnly = true)
    public DeliveryOrder getOrderForEndUser(UUID endUserId, UUID orderId) {
        DeliveryOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found: " + orderId));
        if (!order.getCreatedByEndUserId().equals(endUserId)) {
            throw ApiException.forbidden("You can only view your own orders.");
        }
        return order;
    }

    @Transactional(readOnly = true)
    public OrderDtos.Progress computeProgress(DeliveryOrder order) {
        if (order.getCurrentJobId() == null) return null;
        Job job = jobRepository.findById(order.getCurrentJobId()).orElse(null);
        if (job == null) return null;

        if (job.getAssignedDroneId() == null) return null;
        Drone d = droneRepository.findById(job.getAssignedDroneId()).orElse(null);
        if (d == null || d.getLastLat() == null || d.getLastLng() == null) return null;

        double remainingMeters = GeoUtil.haversineMeters(d.getLastLat(), d.getLastLng(), job.getDropoffLat(), job.getDropoffLng());
        int etaSec = (int) Math.round(remainingMeters / AVERAGE_SPEED_M_PER_S);

        return new OrderDtos.Progress(
                Mapper.latLng(d.getLastLat(), d.getLastLng()),
                Math.max(0, etaSec)
        );
    }

    // -------- Admin flows --------

    @Transactional(readOnly = true)
    public List<DeliveryOrder> listAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public DeliveryOrder adminUpdateOrder(UUID orderId, Double originLat, Double originLng, Double destLat, Double destLng) {
        DeliveryOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.FAILED || order.getStatus() == OrderStatus.CANCELED) {
            throw ApiException.badRequest("Cannot modify an order in terminal state: " + order.getStatus());
        }

        Job currentJob = requireCurrentJob(order);

        // Keep semantics consistent with "withdraw before pickup": after pickup, origin/destination changes are risky.
        if (currentJob.getStatus() == JobStatus.IN_PROGRESS) {
            throw ApiException.badRequest("Cannot modify an order after pickup (job already in progress).");
        }

        // Destination can be updated for any non-terminal order before pickup.
        if (destLat != null && destLng != null) {
            order.setDestination(destLat, destLng);
            currentJob.setDropoff(destLat, destLng);
        }

        // Origin update is meaningful only for the initial leg (pickup at origin).
        if (originLat != null && originLng != null) {
            if (currentJob.getType() != JobType.PICKUP_AND_DELIVER) {
                throw ApiException.badRequest("Cannot change origin during handoff flow (pickup is at broken drone location).");
            }
            order.setOrigin(originLat, originLng);
            currentJob.setPickup(originLat, originLng);
        }

        jobRepository.save(currentJob);
        orderRepository.save(order);
        return order;
    }

    @Transactional(readOnly = true)
    public List<Drone> listDrones() {
        return droneRepository.findAll();
    }

    @Transactional
    public Drone adminSetDroneStatus(UUID droneId, DroneStatus status) {
        Drone d = droneRepository.findById(droneId)
                .orElseThrow(() -> ApiException.notFound("Drone not found: " + droneId));
        if (status == DroneStatus.BROKEN) {
            // Apply the same broken behavior as if the drone reported itself broken.
            markDroneBrokenInternal(d);
        } else {
            d.setStatus(status);
            droneRepository.save(d);
        }
        return d;
    }

    // -------- Drone flows --------

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
        Job job = requireInProgressJobForDrone(droneId, jobId);

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
        Job job = requireInProgressJobForDrone(droneId, jobId);

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
        markDroneBrokenInternal(d);
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
        if (d.getCurrentJobId() == null) return null;
        return jobRepository.findById(d.getCurrentJobId()).orElse(null);
    }

    // -------- Internal helpers --------

    private Job requireCurrentJob(DeliveryOrder order) {
        if (order.getCurrentJobId() == null) {
            throw ApiException.badRequest("Order has no current job.");
        }
        return jobRepository.findById(order.getCurrentJobId())
                .orElseThrow(() -> ApiException.notFound("Current job not found: " + order.getCurrentJobId()));
    }

    private Job requireInProgressJobForDrone(UUID droneId, UUID jobId) {
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
    private void markDroneBrokenInternal(Drone d) {
        d.setStatus(DroneStatus.BROKEN);

        UUID currentJobId = d.getCurrentJobId();
        if (currentJobId == null) {
            droneRepository.save(d);
            return;
        }

        Job currentJob = jobRepository.findById(currentJobId).orElse(null);
        if (currentJob == null) {
            d.setCurrentJobId(null);
            droneRepository.save(d);
            return;
        }

        // Only create a handoff job if the drone currently has the goods (job in progress).
        if (currentJob.getStatus() == JobStatus.IN_PROGRESS) {
            // Fail the in-progress job (it cannot continue)
            currentJob.setStatus(JobStatus.FAILED);
            currentJob.setFailedAt(Instant.now());
            jobRepository.save(currentJob);

            DeliveryOrder order = orderRepository.findById(currentJob.getOrderId())
                    .orElseThrow(() -> ApiException.notFound("Order not found for job: " + currentJob.getOrderId()));

            if (d.getLastLat() == null || d.getLastLng() == null) {
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
