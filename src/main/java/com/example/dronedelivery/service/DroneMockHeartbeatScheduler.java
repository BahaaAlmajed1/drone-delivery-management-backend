package com.example.dronedelivery.service;

import com.example.dronedelivery.domain.Drone;
import com.example.dronedelivery.domain.DroneStatus;
import com.example.dronedelivery.repo.DroneRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

@Service
public class DroneMockHeartbeatScheduler {
    private final DroneRepository droneRepository;

    public DroneMockHeartbeatScheduler(DroneRepository droneRepository) {
        this.droneRepository = droneRepository;
    }

    @Scheduled(fixedDelayString = "#{${app.scheduler.heartbeat-interval-seconds:60} * 1000}")
    @Transactional
    public void sendMockHeartbeats() {
        // TODO: Remove once we have real drones calling the heartbeat API.
        List<Drone> drones = droneRepository.findByStatusIn(EnumSet.of(DroneStatus.ACTIVE, DroneStatus.FIXED));
        Instant now = Instant.now();
        for (Drone drone : drones) {
            drone.setLastHeartbeatAt(now);
        }
        if (!drones.isEmpty()) {
            droneRepository.saveAll(drones);
        }
    }
}
