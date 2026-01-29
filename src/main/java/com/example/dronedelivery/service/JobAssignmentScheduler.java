package com.example.dronedelivery.service;

import com.example.dronedelivery.domain.Drone;
import com.example.dronedelivery.domain.DroneStatus;
import com.example.dronedelivery.domain.Job;
import com.example.dronedelivery.domain.JobStatus;
import com.example.dronedelivery.repo.DroneRepository;
import com.example.dronedelivery.repo.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JobAssignmentScheduler {
    private static final Logger logger = LoggerFactory.getLogger(JobAssignmentScheduler.class);
    private final DroneRepository droneRepository;
    private final JobRepository jobRepository;
    private final DeliveryServiceHelper helper;
    private final long heartbeatTimeoutMinutes;

    public JobAssignmentScheduler(
            DroneRepository droneRepository,
            JobRepository jobRepository,
            DeliveryServiceHelper helper,
            @Value("${app.scheduler.heartbeat-timeout-minutes:5}") long heartbeatTimeoutMinutes
    ) {
        this.droneRepository = droneRepository;
        this.jobRepository = jobRepository;
        this.helper = helper;
        this.heartbeatTimeoutMinutes = heartbeatTimeoutMinutes;
    }

    @Scheduled(fixedDelayString = "${app.scheduler.assignment-interval-seconds:10}000")
    public void assignJobsToClosestDrones() {
        List<Job> openJobs = jobRepository.findByStatusOrderByCreatedAtAsc(JobStatus.OPEN);
        if (openJobs.isEmpty()) {
            return;
        }

        List<Drone> availableDrones = droneRepository
                .findByStatusInAndCurrentJobIdIsNull(EnumSet.of(DroneStatus.ACTIVE, DroneStatus.FIXED))
                .stream()
                .filter(drone -> drone.getLastLat() != null && drone.getLastLng() != null)
                .toList();

        if (availableDrones.isEmpty()) {
            logger.debug("Job assignment skipped: no available drones for {} open jobs.", openJobs.size());
            return;
        }

        Set<UUID> reservedDroneIds = availableDrones.stream()
                .map(Drone::getId)
                .collect(Collectors.toSet());

        for (Job job : openJobs) {
            UUID excludedDroneId = job.getExcludedDroneId();
            Drone closest = availableDrones.stream()
                    .filter(drone -> reservedDroneIds.contains(drone.getId()))
                    .filter(drone -> excludedDroneId == null || !excludedDroneId.equals(drone.getId()))
                    .min(Comparator.comparingDouble(drone -> distanceMeters(job, drone)))
                    .orElse(null);

            if (closest == null) {
                continue;
            }

            boolean assigned = tryAssign(job.getId(), closest.getId());
            if (assigned) {
                logger.info("Assigned job {} to drone {}.", job.getId(), closest.getId());
                reservedDroneIds.remove(closest.getId());
            }
        }
    }

    @Scheduled(fixedDelayString = "#{${app.scheduler.heartbeat-timeout-minutes:5} * 60000}")
    @Transactional
    public void markStaleDronesBroken() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(heartbeatTimeoutMinutes));
        List<Drone> drones = droneRepository.findByStatusIn(EnumSet.of(DroneStatus.ACTIVE, DroneStatus.FIXED));
        int markedBroken = 0;
        for (Drone drone : drones) {
            Instant lastHeartbeatAt = drone.getLastHeartbeatAt();
            if (lastHeartbeatAt == null || lastHeartbeatAt.isBefore(cutoff)) {
                helper.markDroneBrokenInternal(drone);
                markedBroken++;
                logger.warn("Drone {} marked BROKEN due to stale heartbeat (last={}).", drone.getId(), lastHeartbeatAt);
            }
        }
        if (markedBroken > 0) {
            logger.info("Marked {} drone(s) as BROKEN due to stale heartbeats.", markedBroken);
        }
    }

    @Transactional
    public boolean tryAssign(UUID jobId, UUID droneId) {
        try {
            Job job = jobRepository.findById(jobId).orElseThrow();
            if (job.getStatus() != JobStatus.OPEN) {
                return false;
            }

            Drone drone = droneRepository.findById(droneId).orElseThrow();
            if (drone.getStatus() == DroneStatus.BROKEN || drone.getCurrentJobId() != null) {
                return false;
            }

            if (job.getExcludedDroneId() != null && job.getExcludedDroneId().equals(droneId)) {
                return false;
            }

            if (drone.getLastLat() == null || drone.getLastLng() == null) {
                return false;
            }

            job.setAssignedDroneId(droneId);
            job.setStatus(JobStatus.RESERVED);
            job.setReservedAt(Instant.now());
            jobRepository.save(job);

            drone.setCurrentJobId(job.getId());
            droneRepository.save(drone);
            return true;
        } catch (OptimisticLockingFailureException ex) {
            logger.debug("Optimistic lock failure while assigning job {} to drone {}.", jobId, droneId);
            return false;
        }
    }

    private double distanceMeters(Job job, Drone drone) {
        return GeoUtil.haversineMeters(
                job.getPickupLat(),
                job.getPickupLng(),
                drone.getLastLat(),
                drone.getLastLng()
        );
    }
}
